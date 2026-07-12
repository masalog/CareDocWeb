# CareDocWeb インフラ設計書（CDK）

## 統合アーキテクチャ設計方針

CloudFront を公開エンドポイントとし、静的配信（S3）と API（Lambda）を
同一ドメイン配下に統合する。各リソースが単一の責務のみを持つよう設計する。

### 目標構成

```
ブラウザ
   │ HTTPS
   ▼
CloudFront（公開入口・HTTPS終端・CDN・ルーティング）
   ├─ /api/*  → API Gateway → Lambda（API処理のみ）
   └─ /*      → S3（静的ファイル配信のみ）
```

### 責務分離（各リソースは1つの責務のみ）

| リソース | 責務 | 持たないもの |
|---------|------|-------------|
| CloudFront | 公開エンドポイント・HTTPS終端・パスルーティング | ビジネスロジック |
| S3 | 静的ファイル（HTML/CSS/JS）の配信 | 動的処理 |
| API Gateway | API のゲートウェイ・スロットリング（コスト保護） | ビジネスロジック |
| Lambda | API のビジネスロジックのみ | **静的ファイル（jar から static/ を除外）** |

> ⚠️ 現状の課題：Lambda用 fat-jar に `static/`（フロントHTML）が同梱されており、
> API Gateway 経由でアクセスすると Lambda が静的HTMLを返してしまう。
> これは「Lambda = API専用」という責務分離に反するため、jar から static/ を除外する。
> （※ CloudFront統合は完了済み。/api/* はAPI Gatewayへ振り分けられ実害はないが、純化のため除外を仕上げ作業として残している。）

### 依存方向（一方向・循環させない）

```
BackendStack（API）───[ exportValue: API URL ]───▶ FrontendStack（CloudFront）
```

- FrontendStack が BackendStack の API ドメインを参照する **一方向依存**
- 逆方向（BackendStack → FrontendStack）の参照は作らない → 循環依存を防止

### パス設計

| ブラウザから見たパス | CloudFront の振り分け | 転送先 |
|---------------------|----------------------|--------|
| `/` `/index.html` `/admin.html` `/style.css` `/app.js` | デフォルトビヘイビア | S3 |
| `/api/*` | 専用ビヘイビア（キャッシュ無効） | API Gateway |

- フロントエンド（app.js）は API を **相対パス `/api/...`** で呼ぶ
  → フロントは API の物理的な場所（API Gateway のURL）を知らない = 疎結合
- CloudFront 側で `/api/*` を API Gateway のステージ（`prod`）へマッピング

### コスト保護の方式（APIキー → ステージスロットリングへ移行）

| 段階 | 方式 | 備考 |
|------|------|------|
| 旧構成 | 使用量プラン + APIキー | メソッド側で `apiKeyRequired` を設定していなかったため、キー無しリクエストには効かない“飾り”になっていた |
| **現構成** | **ステージレベルのスロットリング**（10req/秒・バースト20） | 全リクエストに無条件で効く。Stage 移行で論理IDが変わった UsagePlanKey の 409 競合も解消 |

> スロットリングは「認証」ではなく「使用量制御・コスト保護」。

---

## 概要

CareDocWeb のインフラを **AWS CDK（Java）** でコード化（IaC）する。

フロントエンド（HTML + CSS + JavaScript）を **S3 + CloudFront** で静的ホスティングし、
バックエンド（Spring Boot）を **Lambda + API Gateway** で API 化する。
最終的に CloudFront を公開エンドポイントとして両者を統合する。

スタックは責務ごとに **FrontendStack**（配信）と **BackendStack**（API）に分離する。

---

## 技術選定

| 項目 | 技術 | 選定理由 |
|------|------|---------|
| IaC | AWS CDK（Java） | バックエンドと同じ言語で統一。「Javaで一気通貫」をアピール |
| 言語 | Java 21 | CareDocWeb本体と統一 |
| ビルド | Maven | 本体と同じビルドツール |
| ホスティング | S3 + CloudFront | HTTPS対応・CDN高速化・S3直アクセス遮断 |
| リージョン | ap-northeast-1（東京） | 国内利用のためレイテンシ最小 |

---

## システム構成図

```
      インターネット
           │ HTTPS
           ▼
   ┌───────────────────┐
   │   CloudFront      │  ← CDN + HTTPS証明書（デフォルト証明書）
   │  (Distribution)   │  ← 唯一の公開エンドポイント
   └───────────────────┘
       ┌───┴────────────────────┐
       │ /*                     │ /api/*
       ▼ OAC                    ▼
┌───────────────────┐   ┌───────────────────┐
│   S3 バケット      │   │   API Gateway      │  ← REST API + ステージスロットリング
│  (静的ファイル)    │   │   (LambdaRestApi)  │
│  index.html       │   └───────────────────┘
│  admin.html       │           │
│  style.css        │           ▼
│  app.js           │   ┌───────────────────┐
└───────────────────┘   │  Lambda            │  ← Spring Boot + SnapStart
 ※パブリックアクセス      │  (API処理のみ)     │     メモリ2GB
   全ブロック（非公開）    └───────────────────┘
                                │ JPA / SSM
                                ▼
                        ┌───────────────────┐
                        │  Supabase          │  ← PostgreSQL
                        │  SSM Parameter Store│    (接続情報を実行時取得)
                        └───────────────────┘
```

---

## 構築するAWSリソース

### FrontendStack（配信）

| リソース | 役割 | 主な設定 |
|---------|------|---------|
| **S3バケット** | 静的ファイル格納 | パブリックアクセス全ブロック、バージョニング任意 |
| **CloudFront Distribution** | HTTPS配信・CDN | デフォルトルートオブジェクト = index.html |
| **OAC (Origin Access Control)** | S3へのアクセス制御 | CloudFrontからのS3アクセスのみ許可 |
| **BucketDeployment** | ファイル自動アップロード | static/ の内容をS3に配置、デプロイ時にキャッシュ無効化 |

### BackendStack（API）

| リソース | 役割 | 主な設定 |
|---------|------|---------|
| **Lambda関数** | Spring Boot の API 処理 | Java 21、メモリ2GB、タイムアウト30秒、SnapStart有効 |
| **API Gateway (REST API)** | API のゲートウェイ | LambdaRestApi（全パスをLambdaにプロキシ）、`binaryMediaTypes("*/*")` |
| **ステージスロットリング** | 使用量制御・コスト保護 | 10req/秒、バースト20（ステージ全体・全リクエストに適用） |
| **SSM Parameter Store 参照** | DB接続情報の実行時取得 | url/username/password の3つ（password は SecureString） |
---

## セキュリティ設計

| 対策 | 内容 |
|------|------|
| S3パブリックアクセス遮断 | `blockPublicAccess: BLOCK_ALL`。バケットは完全非公開 |
| OACによるアクセス制御 | S3への直リンク（`.s3.amazonaws.com`）は403。CloudFront経由のみ許可 |
| HTTPS強制 | CloudFrontで `redirect-to-https`。HTTP → HTTPS 自動リダイレクト |
| バケットポリシー | CloudFrontのサービスプリンシパルからのGetObjectのみ許可 |

---

## CDKプロジェクト構成

```text
CareDocWeb/
└── cdk/
    ├── pom.xml                              ← CDK用Maven設定
    ├── cdk.json                             ← CDKアプリのエントリ設定
    └── src/
        └── main/
            └── java/
                └── com/example/cdk/
                    ├── AppStage.java
                    ├── BackendStack.java       ← Lambda + API Gateway スタック（API）                 
                    ├── CareDocWebCdkApp.java   ← エントリポイント（App定義）
                    ├── FrontendStack.java      ← S3 + CloudFront スタック（配信）
                    └── PipelineStack.java
```

---

## スタック設計（FrontendStack）

### 責務
フロントエンドの静的ホスティング基盤（S3 + CloudFront + OAC）を定義する。

### 主な構成要素

| 要素 | CDK Construct | 説明 |
|------|--------------|------|
| S3バケット | `Bucket` | 静的ファイル格納。パブリックアクセス遮断 |
| CloudFront | `Distribution` | S3をオリジンにHTTPS配信 |
| オリジン | `S3BucketOrigin.withOriginAccessControl()` | OAC付きでS3を参照 |
| デプロイ | `BucketDeployment` | `../src/main/resources/static` をS3にアップロード |

### 出力（CfnOutput）

| 出力名 | 内容 |
|--------|------|
| `DistributionUrl` | CloudFrontのURL（`https://dre5onrtbrgty.cloudfront.net`） |

---

## デプロイ手順

```powershell
# cdk ディレクトリに移動
cd C:\Users\dghy1\IdeaProjects\CareDocWeb\cdk

# 差分確認（何が作られるか事前チェック）
npx cdk diff

# デプロイ実行
npx cdk deploy

# 削除（不要になったら）
npx cdk destroy
```

---

## 前提条件（セットアップ済み）

| 項目 | バージョン | 状態 |
|------|-----------|------|
| Node.js | v24.14.1 | ✅ |
| AWS CLI | 2.31.32 | ✅ |
| CDK CLI | 2.1129.0 | ✅ |
| AWS認証 | 環境変数 `CDK_DEFAULT_ACCOUNT` / AWS CLIプロファイルで設定（ドキュメントには記載しない） | ✅ |
| CDK bootstrap | ap-northeast-1 | ✅ 完了 |

---

## 段階的構築ロードマップ

| Phase | 範囲 | 状態 |
|-------|------|------|
| **① フロントエンド** | S3 + CloudFront | ✅ 完了 |
| ② バックエンド | Lambda（Spring Boot + SnapStart） | ✅ 完了 |
| ③ API公開 | API Gateway（ステージスロットリングによるレート制限） | ✅ 完了 |
| ④ 統合 | CloudFront から `/api/*` を API Gateway へルーティング | ✅ 完了 |

### Phase ④ の実装順序（責務分離を保つ順）

統合は「まず疎通を通す → 仕上げで純化・軽量化」の順で実装した。

| 順 | 作業 | 状態 | 設計上の意味 |
|----|------|------|-------------|
| **④-1** | BackendStack が `LambdaRestApi` を生成し、`getApi()` で公開 | ✅ | 一方向依存の受け渡し口を用意 |
| **④-2** | エントリで Backend→Frontend の順に生成し、api オブジェクトを直接渡す（A-3方式） | ✅ | スタック分離を保ったままオブジェクト参照で確実に統合。CDKが Export/ImportValue を自動生成 |
| **④-3** | FrontendStack に `/api/*` ビヘイビア追加（`RestApiOrigin` + キャッシュ無効 + 全メソッド許可） | ✅ | 単一ドメイン統合・CORS不要化。ステージパス(/prod)はCDKが自動解決 |
| **④-4** | SPAフォールバック（403/404→index.html）を削除 | ✅ | ディストリビューション全体に効くため、/api/* のエラーがHTML化される罠を回避 |
| **④-5** | `binaryMediaTypes(List.of("*/*"))` を LambdaRestApi に追加 | ✅ | PDF等バイナリのBase64破損を解消（curl・ブラウザ両方で確認済み） |
| **④-6** | app.js の fetch は相対パス `/api/...`（既に対応済み） | ✅ | フロントとAPIの疎結合。無修正で動作 |

> **実装上の要点**
> - `RestApiOrigin` の引数型は `LambdaRestApi`（`RestApiBase`）にすること。`IRestApi`（インターフェース）ではコンパイルエラーになる。
> - `binaryMediaTypes("*/*")` はクライアントの `Accept` ヘッダーに応じてバイナリ変換する。ブラウザの自動Accept(`*/*`)でPDFは正常配信されたため app.js への Accept 追加は不要だった。JSON API（/api/members）も `*/*` 下で正常。

### Phase ④ の残作業（仕上げ・任意）

| 作業 | 設計上の意味 |
|------|-------------|
| Lambda用 jar から `static/` を除外 | Lambda を「API専用」に純化・軽量化（統合完成後の仕上げとして実施可能） |

> ※ 旧構成で予定していた「CloudFront Functions での `x-api-key` 注入」は、APIキー方式を廃止しステージスロットリングへ移行したため不要となった。

## Lambda コールドスタート高速化（SnapStart）

Java + Spring Boot の Lambda はコールドスタートが重い（実測 約6.8秒）。以下の知見で改善した。

### ★ 最重要：SnapStart を効かせるにはエイリアス化が必須

`LambdaRestApi` に `Function` を直接渡すと、API Gateway は **`$LATEST`** を呼ぶ。
SnapStart は**公開バージョンのスナップショットにのみ効く**ため、`$LATEST` では
`SnapStartConf.ON_PUBLISHED_VERSIONS` を設定していても**効かない**。

対処：公開バージョン → エイリアス → API Gateway をエイリアスに向ける。

```java
Version apiVersion = apiFunction.getCurrentVersion();   // 公開バージョン発行
Alias apiAlias = Alias.Builder.create(this, "ApiAlias")
        .aliasName("live").version(apiVersion).build();  // エイリアス "live"

LambdaRestApi.Builder.create(this, "RestApi")
        .handler(apiAlias)   // ← $LATEST ではなくエイリアスを呼ぶ
        .build();
```

→ この変更だけでコールドスタート **6.8秒 → 約1.1秒** に短縮（約6倍高速化・追加費用ゼロ）。

### その他の施策（効果と判断）

| 施策 | 効果 | 判断 |
|------|------|------|
| **エイリアス化** | 6.8s→1.1s | ✅ 採用（最重要・主役） |
| `JAVA_TOOL_OPTIONS`=`-XX:+TieredCompilation -XX:TieredStopAtLevel=1` | 理論上はJIT負荷減。だが実測でコールドスタートが約2.7秒に**悪化**（SnapStartは最適化済み状態を保存するため、JITレベル制限が逆効果の可能性）| ❌ 不採用（効果不確実・除外） |
| メモリ 2048→3008MB | CPU増だが**スナップショット復元コストも増**。実測でコールドスタートが 約2.9秒に**悪化** | ❌ 不採用（2048MB維持） |
| メモリ 2048→1024MB | Max Memory Used 379MB のため使用量には余裕。ただしCPUが半減しコールドスタートが遅くなる懸念があり、最も安定して速かった2048MBを維持 | ❌ 不採用（2048MB維持） |
| DB接続 priming（afterRestore） | DB接続の約1.2秒を「初回リクエスト」から「Restore Duration」へ移動させるだけで総時間は減らず、Restore が619ms→**2166msに悪化**（実測） | ❌ 不採用（削除済み） |
| Provisioned Concurrency | コールドスタート実質ゼロ | ❌ 不採用（常時課金・デモに割高） |

> ⚠️ **最終結論：メモリ2048MB + SnapStartエイリアス化のみが最良**（コールド約1.1秒）。
> メモリ増減・JITオプション・primingはいずれも実測で効果なし〜逆効果だった。
> SnapStart環境では「常識的な最適化」が裏目に出やすく、必ず実測で判断すること。

### ⚠️ 計測の注意（重要）

- **デプロイ直後の計測は無効**：新バージョンのスナップショット作成が非同期のため、直後は
  フルコールドスタート（6〜7秒）になる。デプロイ後 **5〜10分待つ**こと。
- 純粋なコールドスタートを測るには、**15〜30分放置**してLambdaをアイドルにしてからアクセスすること。SnapStart有効時の実力値は **約1秒前後**。

### ボトルネック分析（CloudWatch Logs の REPORT 行で切り分け）

コールドスタートが約2.7秒残る原因を、CloudWatch Logs の `REPORT` 行で切り分けた結果、
**主犯は初回の DB 接続確立（約1.2秒）** と判明した。メモリ増強・JITオプションが
効かなかったのは、それらが DB 接続時間に影響しないためだった。

**コールドスタート時の実測内訳（メモリ2048MB・エイリアス化のみ）:**

| 項目 | 値 | 内容 |
|------|:---:|------|
| Restore Duration | 619ms | SnapStart スナップショット復元（十分速い） |
| Duration（コールド） | 1521ms | Lambda内処理（Spring処理 + **初回DB接続** + クエリ） |
| Duration（ウォーム） | 323ms | 2回目以降の処理（DB接続済み） |
| Max Memory Used | 379MB | 2048MB中379MBのみ使用（メモリ増強が無意味な裏付け） |
| curl total | 約2710ms | 上記 + ネットワーク往復（CloudFront/API GW経由 約570ms） |

**決定的な差分:** コールド Duration 1521ms − ウォーム Duration 323ms = **約1200ms**。
これが「初回だけ発生する処理」＝ほぼ**初回DB接続確立**（Supabase接続＋コネクションプール初期化）。

> 教訓: 「Measure, don't guess」。推測（メモリ・JIT）ではなく CloudWatch の REPORT 行で
> 実測して初めて主犯（初回DB接続）を特定できた。なお `Max Memory Used 379MB` から
> 1024MB への削減も検討したが、CPU半減でコールドスタートが遅くなる懸念があり、
> 最も安定して速かった **2048MB を維持**する判断とした。

### DB接続 priming を試した結果（不採用）

主犯が初回DB接続と分かったため、SnapStart の `afterRestore` フック（CRaC `Resource`）で
復元直後に DB 接続を温める priming を実装・実測した。**結果は逆効果で不採用**とした。

- **なぜ逆効果か**: priming はDB接続の約1.2秒を「初回リクエスト」から「Restore Duration」へ
  **移動させるだけ**で、絶対コストは消えない。実測で Restore が 619ms→**2166ms** に増加し、
  トータルではむしろ悪化した。
- **`org.crac` のスコープ注意**: `aws-serverless-java-container` は `org.crac` を実行環境に
  露出しない。`provided` にすると実行時に `org/crac/Resource.class` が見つからず起動失敗する
  （**compile スコープで fat-jar に同梱が必須**）。ただし今回は priming 自体を不採用としたため
  `org.crac` 依存も `DataSourcePrimer` も削除済み。

> 結論: DB接続の1.2秒は priming では消せない。2.7秒（初回のみ・2回目以降0.6秒）は
> 実用上許容範囲であり、初回も速くしたい場合は **定期ウォームアップ**（コールドスタートを
> そもそも発生させない）が有効な選択肢。

### 定期ウォームアップ（EventBridge  cron）

コールドスタートを「速くする」のではなく「**そもそも発生させない**」アプローチ。
外部から定期的にエンドポイントを叩き、Lambda をアイドル回収させずウォーム状態に保つ。
priming が「DB接続コストを移動するだけ」で失敗したのに対し、こちらは
ユーザーが待たない時間帯（バックグラウンド）にコールドスタートを消化する発想。

**構成（最もシンプルな CloudFront経由 × EventBridge  cron）:**

```
EventBridge （5分ごとの cron）
    │ curl
    ▼
CloudFront /api/health（/api/* なのでキャッシュ無効）→ API Gateway → エイリアス → Lambda
```

**間隔の判断：5分ごと（EventBridge cron の最短）**
- Lambda のアイドル回収は 5〜15分の幅があり、10分間隔だと回収→コールドに当たる回が残る。
  5分間隔なら回収より早く叩き続けられ、ウォーム維持がより確実。
- コスト：月約8,640回。Lambda無料枠（月100万リクエスト）の約0.86%で無料枠内。

> なぜ Lambda直接Invoke や API Gateway直ではなく CloudFront経由か：
> ユーザーと**完全に同一経路**（CloudFront→API GW→エイリアス→Lambda）を温められ、
> SnapStartのエイリアス経由とも整合し、URLを1つ叩くだけで最もシンプルなため。

### 今後

- CI/CD：CodeBuild と CodePipeline で `cdk deploy` を自動化

---

## 補足：将来の拡張

- **フロントエンド・バックエンド統合**: CloudFront と API Gateway を統合して、Lambda に直接アクセスできないようにする
