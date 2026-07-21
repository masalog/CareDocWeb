# CareDocWeb インフラ設計書

## 概要

CareDocWeb のインフラを **AWS CDK（Java）** でコード化（IaC）し、
CloudFront を公開エンドポイントとして、静的配信（S3）と API（Lambda）を
同一ドメイン配下に統合する。スタックは責務ごとに **FrontendStack**（配信）と
**BackendStack**（API）に分離する。各リソースが単一の責務のみを持つよう設計する
（構成図は「システム構成図」を参照）。

### 責務分離（各リソースは1つの責務のみ）

| リソース | 責務 | 持たないもの |
|---------|------|-------------|
| CloudFront | 公開エンドポイント・HTTPS終端・パスルーティング | ビジネスロジック |
| S3 | 静的ファイル（HTML/CSS/JS）の配信 | 動的処理 |
| API Gateway | API のゲートウェイ・スロットリング（コスト保護） | ビジネスロジック |
| Lambda | API のビジネスロジックのみ | 静的ファイル（現状 jar に同梱、「今後」参照） |

### 依存方向（一方向・循環させない）

```text
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
| **現構成** | **ステージレベルのスロットリング**（10req/秒・バースト20） | APIキー不要でステージ配下に適用するレート制御。月間総量・請求額の上限ではない |

> スロットリングは「認証」ではなく「レート制御」。コスト上限や濫用対策は別途検討する。

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

```text
      インターネット
           │ HTTPS
           ▼
   ┌───────────────────┐
   │   CloudFront      │  ← CDN + HTTPS証明書（デフォルト証明書）
   │  (Distribution)   │  ← 公開エンドポイント
   └───────────────────┘
       ┌───┴────────────────────┐
       │ /*                     │ /api/*
       ▼ OAC                    ▼
┌───────────────────┐   ┌───────────────────┐   ┌───────────────────┐
│   S3 バケット      │   │   API Gateway      │──▶│  Cognito           │
│  (静的ファイル)    │   │   (LambdaRestApi)  │   │  User Pool         │
│  index.html       │   │  ※アクセスログ出力  │   │  （/api/admin/* のみ│
│  admin.html       │   └───────────────────┘   │   認証必須）        │
│  style.css        │           │                └───────────────────┘
│  app.js           │           ▼
└───────────────────┘   ┌───────────────────┐
 ※パブリックアクセス      │  Lambda            │  ← Spring Boot + SnapStart
   全ブロック（非公開）    │  (API処理のみ)     │     メモリ2GB
                        └───────────────────┘
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
| **CloudFrontInvalidation（Custom Resource）** | インフラ更新時のキャッシュ無効化 | スタック作成・更新のたびに `/*` を無効化。静的ファイル自体のS3配置は行わない（後述のPipelineStack Postステップが担当） |

### BackendStack（API）

| リソース | 役割 | 主な設定 |
|---------|------|---------|
| **Lambda関数** | Spring Boot の API 処理 | Java 21、メモリ2GB、タイムアウト30秒、SnapStart有効 |
| **API Gateway (REST API)** | API のゲートウェイ | LambdaRestApi（全パスをLambdaにプロキシ）、`binaryMediaTypes("*/*")` |
| **ステージスロットリング** | 使用量制御・コスト保護 | 10req/秒、バースト20（ステージ全体・全リクエストに適用） |
| **アクセスログ (LogGroup)** | 認証監査 | requestId/requestTime/ip/method/path/status/authorizerError/cognitoUser(email) を記録、保持期間1ヶ月 |
| **SSM Parameter Store 参照** | DB接続情報の実行時取得 | url/username/password の3つ（password は SecureString） |

### AuthConstruct（Cognito）

管理画面（`/api/admin/**`）専用の認証基盤。BackendStack が内部で生成する。

| リソース | 役割 | 主な設定 |
|---------|------|---------|
| **UserPool** | 管理者アカウントの管理 | セルフサインアップ無効（管理者はコンソール/CLIから手動追加）、メールでサインイン、パスワードポリシー（8文字以上・大小英数字必須）、`RemovalPolicy.RETAIN`（スタック削除時もユーザー情報を保持） |
| **UserPoolClient** | フロントエンド（admin.html）からの認証 | SRP認証フロー、シークレットなし（ブラウザSPAのため） |
| **CognitoUserPoolsAuthorizer** | `/api/admin/{proxy+}` の認可 | 有効な ID トークンがないリクエストは Lambda に到達しない。他パスは従来どおり認証なし |

UserPool ID・App Client ID は `CfnOutput` で出力し、フロントエンドの Cognito 認証実装に利用する。

### PipelineStack（CI/CD）

| リソース | 役割 | 主な設定 |
|---------|------|---------|
| **CodePipeline（V2）** | CDK Pipelines によるセルフミューテーティングパイプライン | `main` への push で Synth → SelfMutate → Deploy → Post を自動実行 |
| **CodeStarConnections** | GitHub リポジトリ（`masalog/CareDocWeb`）への接続 | 初回のみ AWS コンソールでの承認が必要 |
| **Synth（CodeBuildStep）** | ビルド・テスト・cdk synth | `mvn package`（テスト実行含む）→ `cdk synth`。テスト失敗時はここで停止 |
| **Post（CodeBuildStep）** | 静的ファイルの反映 | `aws s3 sync --delete` + CloudFront キャッシュ無効化。バケット名・Distribution ID は FrontendStack の CfnOutput から注入 |

設計の背景・各ステージの詳細は「CI/CD（CDK Pipelines）」セクションを参照。

---

## セキュリティ設計

| 対策 | 内容 |
|------|------|
| S3パブリックアクセス遮断 | `blockPublicAccess: BLOCK_ALL`。バケットは完全非公開 |
| OACによるアクセス制御 | S3への直リンク（`.s3.amazonaws.com`）は403。CloudFront経由のみ許可 |
| HTTPS強制 | CloudFrontで `redirect-to-https`。HTTP → HTTPS 自動リダイレクト |
| バケットポリシー | CloudFrontのサービスプリンシパルからのGetObjectのみ許可 |

管理画面認証（Cognito）の詳細は「構築するAWSリソース」の AuthConstruct、
アクセスログの詳細は同 BackendStack を参照。

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
                    ├── AuthConstruct.java      ← Cognito User Pool（管理画面認証）
                    ├── BackendStack.java       ← Lambda + API Gateway スタック（API）                 
                    ├── CareDocWebCdkApp.java   ← エントリポイント（App定義）
                    ├── FrontendStack.java      ← S3 + CloudFront スタック（配信）
                    └── PipelineStack.java      ← CDK Pipelines（CI/CD）
```

---

## デプロイ手順

```powershell
# リポジトリルートから、cdk ディレクトリに移動
cd cdk

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
| ⑤ 管理画面認証 | Cognito User Pool + `CognitoUserPoolsAuthorizer` を `/api/admin/**` に適用 | ✅ 完了 |
| ⑥ CI/CD | CDK Pipelines（自己更新パイプライン）による `cdk deploy` 自動化 | ✅ 完了 |

### Phase ④ の実装ポイント

- BackendStack が生成した `LambdaRestApi` オブジェクトを FrontendStack に直接渡し、スタック分離を保ったまま統合（CDKが Export/ImportValue を自動生成）
- `RestApiOrigin` には `LambdaRestApi`（`RestApiBase`）を渡す必要がある（`IRestApi` ではコンパイルエラー）。SPAフォールバック（403/404→index.html）は `/api/*` のエラーもHTML化してしまうため削除し、`binaryMediaTypes("*/*")` でPDF等バイナリのBase64破損を解消した

## CI/CD（CDK Pipelines）

**なぜ CDK Pipelines か**: プレーンな CodePipeline + buildspec.yml を手書きするのではなく、
インフラと同じ Java/CDK でパイプライン自体も定義する。パイプライン定義の変更も
`main` への push だけで反映される「セルフミューテーティング」が最大の利点（後述）。

### パイプラインの4ステージ

`main` への push で以下がすべて自動実行される。

1. **Synth**（`CodeBuildStep`）: リポジトリを取得し `mvn package`（テスト実行含む。
   テスト失敗時はここでパイプラインが停止する）→ `cdk synth`
2. **SelfMutate**: Synth の出力（`cdk.out`）にパイプライン自身の定義変更が含まれていれば、
   後続ステップを実行する前にパイプラインを最新の定義へ更新する
3. **Deploy**（`AppStage`）: `BackendStack` → `FrontendStack` の順に CloudFormation スタックを更新
4. **Post**（`CodeBuildStep`）: 静的ファイルを S3 に反映（下記）

パイプライン自体の初回作成のみ手元から `cdk deploy` が必要。以降はアプリのコードも
パイプライン定義自体の変更も、push だけで自動反映される。

### 静的ファイルのデプロイは Post ステップが担当（BucketDeployment は使わない）

FrontendStack では CDK の `BucketDeployment` は使わず、S3 への配置は Deploy 後の
Post ステップで `aws s3 sync --delete` を実行して行う。バケット名・CloudFront
Distribution ID は FrontendStack の `CfnOutput` から `envFromCfnOutputs` で環境変数に
注入する（クロスステージ参照の正規の方法）。IAM ポリシーはバケットの ARN が synth
時点では確定しないため、命名規則（`caredocwebfrontendstack-sitebucket*`）でスコープを
絞っている。実行前に環境変数と同期元ディレクトリの存在を `test` コマンドで検証し、
空振り（バケット名が空のまま sync してしまう等）を防いでいる。

### キャッシュ無効化は2箇所で行う

- **FrontendStack 側**（Custom Resource）: スタックの作成・更新時に発火。CloudFront の
  ビヘイビア設定などインフラ側の変更を反映する
- **Post ステップ側**: `aws cloudfront create-invalidation` を実行。静的ファイルの
  内容更新（コードpush）を反映する

インフラ変更とコンテンツ更新は別々のタイミングで発生するため、片方だけでは
キャッシュが古いまま残るケースが生じる。両方に無効化処理を持たせることで漏れを防ぐ。

### その他の設計判断

- **PipelineType.V2**: 未指定時は feature flag が無効だと V1 になり警告が出る。V2 は
  実行時間ベースの課金（無料枠あり）で、実行頻度の低い本用途では V1 の月額固定課金より低コスト
- **GitHub 接続（CodeStarConnections）**: 初回のみ AWS コンソールでの承認が必要。以降は
  自動で `main` の push を検知する

## Lambda コールドスタート高速化（SnapStart）

Java + Spring Boot の Lambda はコールドスタートが重い（フルコールド 約6.8秒）。
SnapStart は**公開バージョンのスナップショットにのみ効く**ため、`LambdaRestApi` に
`Function` を直接渡すと `$LATEST` が呼ばれて効果が出ない。公開バージョン→エイリアス
経由で呼び出すことで有効化する。

```java
Version apiVersion = apiFunction.getCurrentVersion();   // 公開バージョン発行
Alias apiAlias = Alias.Builder.create(this, "ApiAlias")
        .aliasName("live").version(apiVersion).build();  // エイリアス "live"

LambdaRestApi.Builder.create(this, "RestApi")
        .handler(apiAlias)   // ← $LATEST ではなくエイリアスを呼ぶ
        .build();
```

→ この変更だけでコールドスタート **6.8秒 → 約1.1秒** に短縮（メモリ2048MB維持・追加費用ゼロ）。

**検討したが不採用にした施策**

- メモリ 1024MB/3008MB への変更：いずれも実測でコールドスタートが悪化（2048MBが最速）
- JITオプション（`TieredStopAtLevel=1`）：実測で約2.7秒に悪化
- DB接続 priming（SnapStart `afterRestore` フックで復元直後にDB接続を温める）：DB接続コストを
  「初回リクエスト」から「Restore Duration」に移動させるだけで総時間は減らず、むしろ悪化
- Provisioned Concurrency：コールドスタートは実質ゼロだが常時課金でデモ用途には割高

**ボトルネック**: CloudWatch Logs の `REPORT` 行で実測した結果、コールドスタートに残る
約1.2秒の主因は初回の DB 接続確立（コールド時 Duration 1521ms、ウォーム時 323ms）。
メモリ・JITの調整はDB接続時間に影響しないため効果がなかった。

**定期ウォームアップ**: EventBridge cron（5分ごと）で CloudFront経由に `/api/health` を叩き、
Lambda のアイドル回収によるコールドスタートを抑制する（実行環境の維持を保証するものではなく、ベストエフォート）。
コストは Lambda 無料枠の約0.86%（月約8,640回）。

> ⚠️ 計測時の注意：デプロイ直後はスナップショット作成が非同期のためフルコールドになる。
> 5〜10分待ってから計測すること。
