package com.example.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.Environment;
import software.constructs.Construct;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.CodeBuildStep;
import software.amazon.awscdk.pipelines.ConnectionSourceOptions;
import software.amazon.awscdk.pipelines.StageDeployment;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codestarconnections.CfnConnection;
import software.amazon.awscdk.services.iam.PolicyStatement;

/**
 * CDK Pipelines（セルフミューテーティング）による CI/CD スタック。
 *
 * main への push で以下がすべて自動実行される：
 *   1. Synth      : mvn package（Lambda 用 jar 生成）+ cdk synth
 *   2. SelfMutate : パイプライン自身を最新の定義に更新
 *   3. Deploy     : BackendStack → FrontendStack を CloudFormation で更新
 *   4. Post       : 静的ファイルを S3 に sync + CloudFront キャッシュ無効化
 *
 * このスタック自体の初回デプロイのみ手元から `cdk deploy` する。
 * 以降はパイプライン定義の変更も push だけで自己反映される。
 */
public class PipelineStack extends Stack {

    public PipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // ---- GitHub 接続（デプロイ後にコンソールで承認が必要） ----
        CfnConnection gitHubConnection = CfnConnection.Builder.create(this, "GitHubConnection")
                .connectionName("CareDocWebConnection")
                .providerType("GitHub")
                .build();

        // ---- パイプライン本体 ----
        CodePipeline pipeline = CodePipeline.Builder.create(this, "Pipeline")
                .pipelineName("CareDocWebPipeline")
                // Synth ステップ: リポジトリを取得し、アプリの jar をビルドしてから synth する
                .synth(CodeBuildStep.Builder.create("Synth")
                        .input(CodePipelineSource.connection(
                                "masalog/CareDocWeb", "main",
                                ConnectionSourceOptions.builder()
                                        .connectionArn(gitHubConnection.getAttrConnectionArn())
                                        .build()))
                        // Java 21 を明示（Maven ビルドと synth の両方で使用）
                        .partialBuildSpec(BuildSpec.fromObject(Map.of(
                                "phases", Map.of(
                                        "install", Map.of(
                                                "runtime-versions", Map.of(
                                                        "java", "corretto21"))))))
                        .commands(List.of(
                                // アプリのビルド + テスト実行。テスト失敗時はここでパイプラインが停止する。
                                // -q は付けない: テスト結果(Tests run: ...)をビルドログに証跡として残すため
                                "mvn package",
                                "cd cdk",
                                "npm install -g aws-cdk",
                                "cdk synth"))
                        // cdk.out の場所（リポジトリルートからの相対パス）
                        .primaryOutputDirectory("cdk/cdk.out")
                        .build())
                .build();

        // ---- アプリケーションステージ（Backend + Frontend）を追加 ----
        AppStage appStage = new AppStage(this, "App", StageProps.builder()
                .env(Environment.builder()
                        .account(this.getAccount())
                        .region(this.getRegion())
                        .build())
                .build());

        StageDeployment deployment = pipeline.addStage(appStage);

        // ---- Post ステップ: 静的ファイルの S3 配置 + CloudFront 無効化 ----
        // バケット名と Distribution ID は FrontendStack の CfnOutput から
        // 環境変数として注入される（クロスステージ参照の正規の方法）。
        deployment.addPost(CodeBuildStep.Builder.create("DeployStaticFiles")
                .envFromCfnOutputs(Map.of(
                        "SITE_BUCKET", appStage.getFrontend().getBucketNameOutput(),
                        "DISTRIBUTION_ID", appStage.getFrontend().getDistributionIdOutput()))
                .commands(List.of(
                        // 事前検証（環境変数と同期元の存在チェック）
                        "test -n \"${SITE_BUCKET}\" || { echo 'ERROR: SITE_BUCKET is empty'; exit 1; }",
                        "test -n \"${DISTRIBUTION_ID}\" || { echo 'ERROR: DISTRIBUTION_ID is empty'; exit 1; }",
                        "test -d src/main/resources/static && [ -n \"$(ls -A src/main/resources/static)\" ] || { echo 'ERROR: static dir missing or empty'; exit 1; }",
                        // 同期 + キャッシュ無効化
                        "aws s3 sync src/main/resources/static \"s3://${SITE_BUCKET}\" --delete",
                        "aws cloudfront create-invalidation --distribution-id \"${DISTRIBUTION_ID}\" --paths \"/*\""))
                .rolePolicyStatements(List.of(
                        PolicyStatement.Builder.create()
                                .actions(List.of("s3:PutObject", "s3:DeleteObject",
                                        "s3:GetObject", "s3:ListBucket"))
                                // バケットはステージ内で生成されるため synth 時点で ARN 不明。
                                // 命名規則（<stack名小文字>-sitebucket*）でスコープを絞る。
                                .resources(List.of(
                                        "arn:aws:s3:::caredocwebfrontendstack-sitebucket*",
                                        "arn:aws:s3:::caredocwebfrontendstack-sitebucket*/*"))
                                .build(),
                        PolicyStatement.Builder.create()
                                .actions(List.of("cloudfront:CreateInvalidation"))
                                .resources(List.of("*"))
                                .build()))
                .build());
    }
}