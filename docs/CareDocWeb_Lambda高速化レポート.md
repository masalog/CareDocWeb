# Lambda コールドスタート高速化（SnapStart）

## 結論（要約）

**改善は成功。画面の「初回だけ異常に待たされる」問題はほぼ解消された。**

このシステム（AWS Lambda）は、しばらく誰も使っていないと裏側のサーバーが一旦停止し、次にアクセスした人だけ起動待ちが発生する仕組みになっている（これを「コールドスタート」と呼ぶ）。改善前は、この起動待ちに当たると **画面表示に約 7 秒** かかっていた。

今回の改善（SnapStart：起動済み状態のスナップショットを保存しておき、そこから瞬時に復元する仕組み＋5 分ごとの自動アクセスでサーバーを起こしておく仕組み）により、実測では：

- **ほとんどのアクセス（95% 以上）は約 0.1 秒以内で応答 — 待ち時間は体感ゼロ。** 典型的な応答（中央値）は **わずか 18 ms** で、これが本改善の最大の成果
- ごくまれ（約 4%）に起動待ちに当たった場合でも **約 7 秒 → 典型 約 1.2 秒（最悪でも約 4 秒）** に短縮
- **追加費用はゼロ**

デモで人に見せる用途としては十分な速さになった。残る課題は、起動直後にデータベースへ最初に接続するときの数秒だが、発生するのはアクセス全体の約 4% のみで、有効な追加対策もないため（後述）、現状のままで問題ないと判断した。

---

以下は技術的な詳細。

## 背景

Java + Spring Boot の Lambda はコールドスタートが重く、対策前はフルコールドで約 **6.8 秒** かかっていた。

SnapStart は **公開バージョンのスナップショットにのみ有効** なため、CDK の `LambdaRestApi` に `Function` を直接渡すと `$LATEST` が呼ばれて効果が出ない。公開バージョン → エイリアス経由で呼び出すことで有効化する。

```java
Version apiVersion = apiFunction.getCurrentVersion();   // 公開バージョン発行
Alias apiAlias = Alias.Builder.create(this, "ApiAlias")
        .aliasName("live").version(apiVersion).build();  // エイリアス "live"

LambdaRestApi.Builder.create(this, "RestApi")
        .handler(apiAlias)   // ← $LATEST ではなくエイリアスを呼ぶ
        .build();
```

## 効果（実測）

CloudWatch Logs の `REPORT` 行および Logs Insights による直近 24 時間の集計（289 リクエスト）に基づく。

| 指標 | 値 |
|---|---|
| コールドスタート発生率 | 約 4.8%（289 回中 14 回） |
| ウォーム時の実行時間 | **中央値 18 ms（95% 以上が 0.1 秒以内）** |
| コールド時の合計所要時間 | 中央値 約 1.2 秒（最良 1.0 秒・最悪 4.2 秒、DB 接続の有無で変動） |
| 対策前（SnapStart なし） | 約 6.8 秒 |

コールド時の所要時間は初回リクエストの内容によって幅がある：

- **DB 接続を伴わないリクエスト**（ヘルスチェック等）: 約 **1.1 秒**
- **DB 接続を伴うリクエスト**: 約 **2.2〜4.2 秒**（初回の DB 接続確立が支配的）

メモリは 2048 MB を維持し、追加費用ゼロで達成。

パーセンタイルで見ると、コールド時の中央値は約 1.2 秒（最良 973 ms・最悪 4.2 秒）であり、「約 1.1 秒」はコールド時の典型値としてほぼ実測どおり。3〜4 秒台は初回に DB 接続確立が重なった少数の外れ値で、コールド自体の発生率も約 4% と低い。

## ボトルネック

コールドスタートに残る時間の主因は **初回の DB 接続確立** である（コールド時 Duration 最大 3450 ms、ウォーム時中央値 18 ms）。メモリ・JIT の調整は DB 接続時間に影響しないため効果がなかった。

## 検討したが不採用にした施策

- **メモリ 1024 MB / 3008 MB への変更**: いずれも実測でコールドスタートが悪化（2048 MB が最速）
- **JIT オプション（`TieredStopAtLevel=1`）**: 実測で約 2.7 秒に悪化
- **DB 接続 priming（SnapStart `afterRestore` フックで復元直後に DB 接続を温める）**: DB 接続コストを「初回リクエスト」から「Restore Duration」に移動させるだけで総時間は減らず、むしろ悪化
- **Provisioned Concurrency**: コールドスタートは実質ゼロになるが常時課金でデモ用途には割高

## 定期ウォームアップ

EventBridge cron（5 分ごと）で CloudFront 経由に `/api/health` を叩き、Lambda のアイドル回収によるコールドスタートを抑制する（実行環境の維持を保証するものではなく、ベストエフォート）。

- コスト: Lambda 無料枠の約 0.86%（月約 8,640 回）
- 効果: 直近 24 時間のコールド率 約 4.8% に抑制（95% 以上がウォーム応答）

## 検証方法

検証は「設定確認 → 経路確認 → 実測」の 3 段階で行う。コマンドはすべて PowerShell 用（Windows）。事前に `aws sso login` 等で認証を済ませ、関数名を変数に入れておく。

```powershell
# 関数名が分からない場合は一覧から確認
aws lambda list-functions --query "Functions[].FunctionName"

$fn = "<関数名>"   # 例: CareDocWebBackendStack-ApiFunctionXXXX-xxxxxxxx
```

### 手順 1: SnapStart の設定確認

SnapStart が関数と公開バージョンに適用されているかを確認する。

```powershell
# 関数本体の設定（ApplyOn が PublishedVersions か）
aws lambda get-function-configuration --function-name $fn --query "SnapStart"

# エイリアス "live" の向き先バージョンを確認
aws lambda list-aliases --function-name $fn --query "Aliases[].{Name:Name,Version:FunctionVersion}"

# 公開バージョン側で最適化が完了しているか（<N> は上で確認したバージョン番号）
aws lambda get-function-configuration --function-name "${fn}:<N>" --query "SnapStart"
```

**期待値**: `{"ApplyOn": "PublishedVersions", "OptimizationStatus": "On"}`

`OptimizationStatus` が `Off` の場合はスナップショット作成中（デプロイ直後）か、SnapStart 非対応の設定。数分待って再確認する。

### 手順 2: 呼び出し経路の確認（$LATEST になっていないか）

SnapStart はエイリアス経由の呼び出しにのみ効く。API Gateway の統合先がエイリアスを指しているかを確認する。

```powershell
aws apigateway get-rest-apis --query "items[].{Id:id,Name:name}"
aws apigateway get-resources --rest-api-id <API_ID> --query "items[].{Id:id,Path:path}"
aws apigateway get-integration --rest-api-id <API_ID> --resource-id <RESOURCE_ID> --http-method ANY --query "uri"
```

**期待値**: URI の末尾が `...function:<関数名>:live/invocations`

`:live` が付いていない場合は `$LATEST` を呼んでおり SnapStart は無効。CDK で `handler(apiAlias)` になっているかを見直す。

### 手順 3: コールドスタートの実測（ログ確認）

コールドを人為的に起こさなくても、過去に実際に発生したコールドスタートの記録がログに残っている。まずはこれを見るのが最も手軽で、実運用の実態も分かる。

```powershell
# 過去24時間のコールドスタート実績（Restore Duration を含む REPORT 行）
aws logs filter-log-events --log-group-name /aws/lambda/$fn `
  --filter-pattern "REPORT" `
  --start-time ([DateTimeOffset]::UtcNow.AddHours(-24).ToUnixTimeMilliseconds()) `
  --query "events[].message" --output text | Select-String "Restore"
```

**REPORT 行の読み方**:

| 項目 | 意味 |
|---|---|
| `Restore Duration: 700 ms` | スナップショットからの復元時間。**この行があれば SnapStart は有効** |
| `Init Duration: 6000 ms` | フル初期化の時間。**この行が出る場合は SnapStart 無効**（$LATEST を呼んでいる） |
| `Duration: 3200 ms` | リクエスト処理時間。コールド初回は DB 接続確立を含むため大きくなる |
| どちらもない | ウォーム実行（既存環境の再利用）。コールドスタートは発生していない |

体感のコールドスタート時間 ≒ `Restore Duration + Duration`。

### 手順 4: 統計サマリーの取得（Logs Insights）

件数が多い場合は個別の行ではなく集計で見る。

```powershell
$qid = aws logs start-query --log-group-name /aws/lambda/$fn `
  --start-time ([DateTimeOffset]::UtcNow.AddHours(-24).ToUnixTimeSeconds()) `
  --end-time ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()) `
  --query-string 'filter @message like /REPORT RequestId/ | parse @message /Restore Duration: (?<restoreMs>[\d.]+) ms/ | stats count(*) as total, count(restoreMs) as cold, pct(@duration, 50) as p50Ms, pct(@duration, 90) as p90Ms, pct(@duration, 99) as p99Ms, max(@duration) as maxMs' `
  --query "queryId" --output text

Start-Sleep 5
aws logs get-query-results --query-id $qid --query "results"
```

**出力の見方**: `total` = 全実行回数、`cold` = コールドスタート回数、`p50Ms` = 中央値（典型的な応答時間）、`p90Ms` / `p99Ms` = 上位 10% / 1% の遅いリクエストのライン、`maxMs` = 最悪ケース。中央値・パーセンタイルは外れ値に引きずられないため、応答時間の実態把握に適している。

コールド実行のみに絞った分布（Restore 込みの体感値）も取れる：

```powershell
--query-string 'filter @message like /REPORT RequestId/ | parse @message /Restore Duration: (?<restoreMs>[\d.]+) ms/ | filter ispresent(restoreMs) | stats count(*) as cold, min(@duration + restoreMs) as coldBestMs, pct(@duration + restoreMs, 50) as coldP50Ms, max(@duration + restoreMs) as coldWorstMs'
```

**今回の実測例（24 時間、289 実行）**:

| 指標 | 値 |
|---|---|
| ウォーム時 中央値 / p90 | 18 ms / 75 ms |
| コールド発生回数 | 12 回（約 4%） |
| コールド時（Restore 込み） 最良 / 中央値 / 最悪 | 973 ms / **1218 ms** / 4171 ms |

コールド時の中央値が約 1.2 秒であり、「約 1.1 秒」はコールド時の典型値としてほぼ正確。3〜4 秒台は初回に DB 接続が重なった少数の外れ値。ウォーム時の中央値が数十 ms・コールド率が数％以下なら、SnapStart とウォームアップが機能していると判断できる。

### 手順 5（任意）: コールドスタートを強制して直接計測

ログに十分な実績がない場合や、変更直後の効果を今すぐ確認したい場合は、環境変数を変えて新バージョンを発行するとスナップショットが再作成され、次回呼び出しが必ずコールドになる。

```powershell
# 1. 環境変数を変更（内容は何でもよい）→ 新バージョン発行 → エイリアスを付け替え
aws lambda update-function-configuration --function-name $fn `
  --environment "Variables={FORCE_COLD='$(Get-Date -UFormat %s)'}"
aws lambda publish-version --function-name $fn --query "Version" --output text
aws lambda update-alias --function-name $fn --name live --function-version <新バージョン番号>

# 2. スナップショット作成完了を待つ（5〜10分）。手順1のコマンドで OptimizationStatus: On を確認

# 3. エイリアス経由で invoke し、REPORT 行を直接取得
aws lambda invoke --function-name "${fn}:live" --payload "{}" NUL --log-type Tail `
  --query "LogResult" --output text |
  ForEach-Object { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($_)) } |
  Select-String "REPORT"
```

補足:

- `--payload "{}"` は API Gateway 形式ではないためアプリ側でエラーになるが、`Restore Duration` の計測自体は正常に行われるので目的には支障ない
- 関数名だけで invoke すると `$LATEST` に行き SnapStart が効かないため、必ず `"${fn}:live"` とエイリアスを付ける

### よくある落とし穴

- **デプロイ直後に計測すると 6 秒台のフルコールドが出る**: スナップショット作成が非同期のため。`OptimizationStatus: On` を確認してから（目安 5〜10 分後）計測する
- **CloudFront 経由の curl で計測する場合**: CDN のキャッシュ・接続再利用のノイズが乗るため、Lambda 単体のボトルネック分析には REPORT 行を使う。エンドツーエンドの体感確認としては有効