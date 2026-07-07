package com.example.cdk;

import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

/**
 * アプリケーション全体（Backend + Frontend）を1つの単位として
 * パイプラインにデプロイさせるためのステージ。
 *
 * stackName を既存スタック名に固定することで、パイプラインからの
 * デプロイが「新規作成」ではなく「既存スタックの更新」になる
 * （リソースの作り直し・URL変更を防ぐ）。
 */
public class AppStage extends Stage {

    /** Post ステップ（静的ファイル配置）から参照するための FrontendStack。 */
    private final FrontendStack frontend;

    public AppStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);

        BackendStack backend = new BackendStack(this, "Backend", StackProps.builder()
                .stackName("CareDocWebBackendStack")   // 既存スタック名を維持
                .description("CareDocWeb バックエンド（Lambda + API Gateway）")
                .build());

        this.frontend = new FrontendStack(this, "Frontend",
                backend.getApi(),
                StackProps.builder()
                        .stackName("CareDocWebFrontendStack")   // 既存スタック名を維持
                        .description("CareDocWeb フロントエンド配信（S3 + CloudFront・/api/* を API Gateway へ統合）")
                        .build());
    }

    public FrontendStack getFrontend() {
        return this.frontend;
    }
}