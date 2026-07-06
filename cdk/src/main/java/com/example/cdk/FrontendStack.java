package com.example.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
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
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

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
        //    src/main/resources/static/ の内容をアップロードし、
        //    デプロイ時に CloudFront キャッシュを無効化する。
        // ------------------------------------------------------------
        BucketDeployment.Builder.create(this, "DeployStaticFiles")
                .sources(List.of(Source.asset("../src/main/resources/static")))
                .destinationBucket(siteBucket)
                // デプロイ後に CloudFront の全パスのキャッシュを無効化
                .distribution(distribution)
                .distributionPaths(List.of("/*"))
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
    }
}
