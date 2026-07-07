package com.example.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * CareDocWeb の CDK アプリケーション エントリポイント。
 *
 * トップレベルに定義するのはパイプラインのみ。
 * Backend / Frontend は AppStage 経由でパイプラインがデプロイする。
 * インフラもコンテンツも main への push だけで本番反映される。
 */
public class CareDocWebCdkApp {

    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv().getOrDefault("CDK_DEFAULT_REGION", "ap-northeast-1"))
                .build();

        new PipelineStack(app, "CareDocWebPipelineStack", StackProps.builder()
                .env(env)
                .description("CareDocWeb CI/CD（CDK Pipelines・インフラ + コンテンツを main への push で自動デプロイ）")
                .build());

        app.synth();
    }
}