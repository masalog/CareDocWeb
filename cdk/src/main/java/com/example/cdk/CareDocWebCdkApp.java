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
        // 依存方向は Backend → Frontend → Pipeline の一方向。
        BackendStack backend = new BackendStack(app, "CareDocWebBackendStack", StackProps.builder()
                .env(env)
                .description("CareDocWeb バックエンド（Lambda + API Gateway）")
                .build());

        // フロントエンド（S3 + CloudFront）スタックを定義。
        // BackendStack の API Gateway を CloudFront の /api/* オリジンとして渡す。
        FrontendStack frontend = new FrontendStack(app, "CareDocWebFrontendStack",
                backend.getApi(),
                StackProps.builder()
                        .env(env)
                        .description("CareDocWeb フロントエンド配信（S3 + CloudFront・/api/* を API Gateway へ統合）")
                        .build());

        // CI/CD: GitHub → CodePipeline → CodeBuild → S3 の一連を1スタックで定義。
        // CodeBuild とパイプラインを分けると循環依存になるため同一スタックにまとめる。
        // FrontendStack の静的サイト用バケットを渡し、CodeBuild に書き込み権限を付与する。
        new PipelineStack(app, "CareDocWebPipelineStack",
                frontend.getSiteBucket(),
                StackProps.builder()
                        .env(env)
                        .description("CareDocWeb フロントエンド CI/CD パイプライン（GitHub → CodeBuild → S3）")
                        .build());

        // CloudFormation テンプレートを合成して終了。
        app.synth();
    }
}
