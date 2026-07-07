package com.example.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.cloudfront.AllowedMethods;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.CachePolicy;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.OriginRequestPolicy;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOrigin;
import software.amazon.awscdk.services.cloudfront.origins.RestApiOrigin;
import software.amazon.awscdk.services.events.ApiDestination;
import software.amazon.awscdk.services.events.Authorization;
import software.amazon.awscdk.services.events.Connection;
import software.amazon.awscdk.services.events.HttpMethod;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.amazon.awscdk.customresources.AwsCustomResource;
import software.amazon.awscdk.customresources.AwsCustomResourcePolicy;
import software.amazon.awscdk.customresources.AwsSdkCall;
import software.amazon.awscdk.customresources.PhysicalResourceId;
import software.amazon.awscdk.services.iam.PolicyStatement;

/**
 * フロントエンド静的ホスティング用スタック。
 *
 * 構成：
 *   CloudFront（HTTPS・CDN）─ OAC ─→ S3（非公開バケット）
 *
 * S3 は完全非公開とし、CloudFront 経由（OAC）でのみアクセスを許可する。
 */
public class FrontendStack extends Stack {

    public FrontendStack(final Construct scope, final String id,
                         final LambdaRestApi api, final StackProps props) {
        super(scope, id, props);

        // ------------------------------------------------------------
        // 1. S3 バケット（静的ファイル格納・完全非公開）
        // ------------------------------------------------------------
        Bucket siteBucket = Bucket.Builder.create(this, "SiteBucket")
                // パブリックアクセスを全てブロック（CloudFront 経由のみアクセス可）
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                // サーバーサイド暗号化（S3 マネージドキー）
                .encryption(BucketEncryption.S3_MANAGED)
                // cdk destroy 時にバケットを削除（ポートフォリオ用途のため）
                .removalPolicy(RemovalPolicy.DESTROY)
                // バケット削除時に中身も自動削除
                .autoDeleteObjects(true)
                .build();

        // ------------------------------------------------------------
        // 2. CloudFront ディストリビューション（HTTPS 配信）
        // ------------------------------------------------------------
        Distribution distribution = Distribution.Builder.create(this, "Distribution")
                .defaultBehavior(BehaviorOptions.builder()
                        // OAC 付きで S3 をオリジンに設定。
                        // これにより S3 バケットポリシーが自動生成され、
                        // CloudFront からのアクセスのみ許可される。
                        .origin(S3BucketOrigin.withOriginAccessControl(siteBucket))
                        // HTTP でのアクセスは HTTPS へリダイレクト
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .build())
                // /api/* は API Gateway（Lambda）へルーティングする。
                // API 応答はキャッシュせず、GET/POST/PUT/DELETE を許可する。
                // これにより CloudFront が唯一の公開エンドポイントとなり、
                // フロントとAPIが同一オリジンになるため CORS は不要になる。
                .additionalBehaviors(Map.of(
                        "/api/*", BehaviorOptions.builder()
                                .origin(new RestApiOrigin(api))
                                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                                .allowedMethods(AllowedMethods.ALLOW_ALL)
                                .cachePolicy(CachePolicy.CACHING_DISABLED)
                                // Host ヘッダーを除く全ビューワー情報をオリジンへ転送する
                                // （API Gateway は Host を独自に解決するため除外が必要）
                                .originRequestPolicy(OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER)
                                .build()))
                // ルート（/）アクセス時に返すファイル
                .defaultRootObject("index.html")
                // 注意: カスタムエラーレスポンス（403/404 → index.html）は設定しない。
                // ディストリビューション全体に効くため、/api/* が返す 403/404 まで
                // index.html(200) に書き換えられ、フロントの res.ok 判定が壊れるため。
                // 本アプリは index.html / admin.html のマルチHTML構成で、
                // クライアントルーティングを持つ真の SPA ではないためフォールバックは不要。
                .comment("CareDocWeb フロントエンド配信")
                .build();

        // ------------------------------------------------------------
        // 3. フロントエンドファイルを S3 にデプロイ
        // ------------------------------------------------------------

        // ❌ BucketDeployment は削除（AWS CLI レイヤーによる脆弱性の原因）
        //    Asset も削除（Java CDK では BucketDeployment と組み合わせると壊れる）

        // ------------------------------------------------------------
        // 3.1 CloudFront キャッシュ無効化（Custom Resource による置き換え）
        // ------------------------------------------------------------

        // CloudFront キャッシュ無効化をデプロイ時に実行する Custom Resource
                AwsCustomResource invalidation = AwsCustomResource.Builder.create(this, "CloudFrontInvalidation")
                        .onCreate(AwsSdkCall.builder()
                                .service("CloudFront")
                                .action("createInvalidation")
                                .parameters(Map.of(
                                        "DistributionId", distribution.getDistributionId(),
                                        "InvalidationBatch", Map.of(
                                                "CallerReference", String.valueOf(System.currentTimeMillis()),
                                                "Paths", Map.of(
                                                        "Quantity", 1,
                                                        "Items", List.of("/*")   // ← キャッシュ無効化パス
                                                )
                                        )
                                ))
                                // デプロイごとに一意の ID を付与
                                .physicalResourceId(PhysicalResourceId.of("Invalidate-" + distribution.getDistributionId()))
                                .build())
                        .policy(AwsCustomResourcePolicy.fromStatements(List.of(
                                PolicyStatement.Builder.create()
                                        .actions(List.of("cloudfront:CreateInvalidation"))
                                        .resources(List.of("*"))   // CloudFront は ARN が特殊なので *
                                        .build()
                        )))
                        .build();

        // ------------------------------------------------------------
        // 4. 出力（デプロイ後にターミナルへ表示）
        // ------------------------------------------------------------
        CfnOutput.Builder.create(this, "DistributionUrl")
                .description("CloudFront の配信 URL")
                .value("https://" + distribution.getDistributionDomainName())
                .build();

        CfnOutput.Builder.create(this, "BucketName")
                .description("作成された S3 バケット名")
                .value(siteBucket.getBucketName())
                .build();

        // ------------------------------------------------------------
        // 5. Lambda 定期ウォームアップ（EventBridge Rule → CloudFront）
        //    SnapStart でも残るアイドルタイムアウト（約15分）対策として、
        //    5分間隔で CloudFront 経由の /api/health を叩き、Lambda を温める。
        //
        //    経路: EventBridge Rule → API Destination (HTTPS)
        //          → CloudFront (/api/health) → API Gateway → alias live → Lambda
        //
        //    本番のユーザーアクセスと完全に同一の経路で温めるため、
        //    CloudFront ドメインは distribution.getDistributionDomainName() で
        //    動的参照する（ハードコードなし・循環依存なし）。
        // ------------------------------------------------------------

        // CloudFront の /api/health を叩くための宛先 URL（動的参照）
        String warmupUrl = "https://" + distribution.getDistributionDomainName() + "/api/health";

        // EventBridge Connection（HTTPS 接続の認証情報を保持する）。
        // /api/health は認証不要だが、Connection は認証設定が必須のため、
        // 影響のないダミーの API キーヘッダーを設定する（health 側は無視する）。
        Connection warmupConnection = Connection.Builder.create(this, "WarmupConnection")
                .authorization(Authorization.apiKey(
                        "x-warmup", SecretValue.unsafePlainText("warmup")))
                .description("Lambda ウォームアップ用の HTTPS 接続（認証は形式的なダミー）")
                .build();

        // API Destination（実際に叩く HTTPS エンドポイントと HTTP メソッド）
        ApiDestination warmupDestination = ApiDestination.Builder.create(this, "WarmupApiDestination")
                .connection(warmupConnection)
                .endpoint(warmupUrl)
                .httpMethod(HttpMethod.GET)
                .description("CloudFront 経由で /api/health を叩き Lambda を温める")
                .build();

        // EventBridge Rule（定期実行 + API Destination ターゲット）。
        // EventBridge Scheduler は API Destination をターゲットに直接指定できず
        //（arn:aws:events:...:api-destination/... は「not in correct format」で弾かれる）、
        // HTTPS 外部 URL を叩くのは EventBridge Rule の領分。
        // targets.ApiDestination を使うと、InvokeApiDestination 権限を持つ
        // IAM ロールは CDK が自動生成するため、ロールの手動定義は不要。
        Rule.Builder.create(this, "WarmupRule")
                // 5分間隔で実行する。
                // 10分間隔では SnapStart の実行環境がリクエストの合間に破棄され、
                // 毎回 RESTORE_START（コールドスタート）が発生することを実測で確認したため、
                // 環境が破棄される前に叩き直せる5分間隔にする。
                // 月約8,640回でも Lambda 無料枠（月100万）の1%未満で無料枠内。
                .schedule(Schedule.rate(Duration.minutes(5)))
                .description("Lambda を定期的に温める（5分間隔・CloudFront 経由 /api/health）")
                // events.ApiDestination（Connection/宛先を定義した construct）を
                // events.targets.ApiDestination でラップして Rule のターゲットにする。
                // import 名の衝突を避けるため targets 側は完全修飾名で指定する。
                .targets(List.of(
                        new software.amazon.awscdk.services.events.targets.ApiDestination(
                                warmupDestination)))
                .build();

        CfnOutput.Builder.create(this, "WarmupUrl")
                .description("ウォームアップで叩く CloudFront 経由の health URL")
                .value(warmupUrl)
                .build();
    }
}
