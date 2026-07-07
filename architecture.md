# システム構成

## ランタイム構成

CloudFront が唯一の公開エンドポイント。`/*` は S3 の静的ファイルへ、`/api/*` は API Gateway 経由で Lambda へルーティングされる(同一オリジンのため CORS 不要)。

```mermaid
flowchart TD
    User([ユーザー]) --> CF["CloudFront<br/>(HTTPS 配信・単一入口)"]
    EB["EventBridge<br/>(5分毎ウォームアップ)"] --> APIDEST["API Destination<br/>(HTTPS)"]
    APIDEST -.->|/api/health| CF
    CF -->|"/*"| S3["S3 サイトバケット<br/>(静的ファイル・非公開/OAC)"]
    CF -->|"/api/*"| APIGW["API Gateway<br/>(REST・ステージスロットリング)"]
    APIGW -->|alias: live| Lambda["Lambda<br/>(Spring Boot・SnapStart)"]
    Lambda -->|接続情報取得| SSM["SSM Parameter Store<br/>(DB 接続情報)"]
    Lambda --> DB[("Supabase<br/>(PostgreSQL)")]
```

## CI/CD パイプライン(CDK Pipelines)

`main` へのマージだけで、テスト実行からインフラ更新・静的ファイル配置・キャッシュ無効化までが全自動で実行される。パイプライン定義自体の変更も SelfMutate により自己反映される。

```mermaid
flowchart TD
    Merge([GitHub main へマージ]) --> Synth

    subgraph Pipeline["CareDocWebPipeline(CDK Pipelines・セルフミューテーティング)"]
        Synth["Synth<br/>(テスト116件・jar 生成・cdk synth)"] --> SelfMutate["SelfMutate<br/>(パイプライン自身を更新)"]
        SelfMutate --> Assets["Assets<br/>(Lambda 用 jar を公開)"]
        Assets --> Backend["Backend 更新<br/>(Lambda + API Gateway)"]
        Backend --> Frontend["Frontend 更新<br/>(S3 + CloudFront)"]
        Frontend --> Static["静的ファイル配置<br/>(s3 sync + キャッシュ無効化)"]
    end
```
