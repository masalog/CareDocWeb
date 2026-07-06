package com.example.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * CareDocWeb の CDK アプリケーション エントリポイント。
 *
 * cdk コマンド（cdk deploy / diff / destroy）はこの main メソッドを実行し、
 * 定義されたスタックを CloudFormation テンプレートに合成（synth）する。
 */
public class CareDocWebCdkApp {

    public static void main(final String[] args) {
        App app = new App();

        // デプロイ先の環境（アカウント・リージョン）を環境変数から取得する。
        // CDK_DEFAULT_ACCOUNT / CDK_DEFAULT_REGION は cdk コマンド実行時に
        // AWS CLI の認証情報から自動的に設定される。
        // リージョンが未設定の場合は東京（ap-northeast-1）をデフォルトにする。
        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv().getOrDefault("CDK_DEFAULT_REGION", "ap-northeast-1"))
                .build();

        // バックエンド（Lambda + API Gateway）スタックを先に定義。
        // 依存方向は Backend → Frontend の一方向（設計書どおり）。
        BackendStack backend = new BackendStack(app, "CareDocWebBackendStack", StackProps.builder()
                .env(env)
                .description("CareDocWeb バックエンド（Lambda + API Gateway）")
                .build());

        // フロントエンド（S3 + CloudFront）スタックを定義。
        // BackendStack の API Gateway を CloudFront の /api/* オリジンとして渡す。
        // これにより CloudFront が唯一の公開エンドポイントとなり、
        // /* → S3、/api/* → API Gateway に振り分けられる。
        new FrontendStack(app, "CareDocWebFrontendStack",
                backend.getApi(),
                StackProps.builder()
                        .env(env)
                        .description("CareDocWeb フロントエンド配信（S3 + CloudFront・/api/* を API Gateway へ統合）")
                        .build());

        // CloudFormation テンプレートを合成して終了。
        app.synth();
    }
}
