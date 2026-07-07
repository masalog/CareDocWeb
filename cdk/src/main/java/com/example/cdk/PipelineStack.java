package com.example.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxBuildImage;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.PipelineType;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeStarConnectionsSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codestarconnections.CfnConnection;

/**
 * フロントエンド CI/CD スタック。
 * GitHub → CodePipeline → CodeBuild → S3（aws s3 sync）の一連を1スタックで定義する。
 *
 * CodeBuild とパイプラインを同一スタックに置く理由：
 *   パイプラインはアーティファクトバケットへの権限を CodeBuild ロールに付与し、
 *   CodeBuild はパイプラインから参照されるため、別スタックにすると循環依存になる。
 */
public class PipelineStack extends Stack {

    public PipelineStack(final Construct scope, final String id,
            Bucket siteBucket, StackProps props) {
        super(scope, id, props);

        // ---- CodeBuild 用 IAM ロール（サイトバケットへの書き込み権限） ----
        Role codeBuildRole = Role.Builder.create(this, "CareDocWebFrontendBuildRole")
                .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
                .build();

        codeBuildRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("s3:PutObject", "s3:DeleteObject", "s3:GetObject", "s3:ListBucket"))
                .resources(List.of(siteBucket.getBucketArn(), siteBucket.getBucketArn() + "/*"))
                .build());

        // ---- CodeBuild プロジェクト ----
        PipelineProject codeBuildProject = PipelineProject.Builder.create(this, "CareDocWebFrontendBuild")
                .projectName("CareDocWebFrontendBuild")
                .environment(BuildEnvironment.builder()
                        .buildImage(LinuxBuildImage.AMAZON_LINUX_2_5)
                        .computeType(ComputeType.SMALL)
                        .environmentVariables(Map.of(
                                "SITE_BUCKET", BuildEnvironmentVariable.builder()
                                        .value(siteBucket.getBucketName())
                                        .type(BuildEnvironmentVariableType.PLAINTEXT)
                                        .build()))
                        .build())
                .buildSpec(BuildSpec.fromSourceFilename("buildspec.yml"))
                .role(codeBuildRole)
                .build();

        // ---- GitHub 接続（デプロイ後にコンソールで承認が必要） ----
        CfnConnection gitHubConnection = CfnConnection.Builder.create(this, "GitHubConnection")
                .connectionName("CareDocWebConnection")
                .providerType("GitHub")
                .build();

        // ---- パイプライン ----
        Artifact sourceOutput = new Artifact();

        Pipeline pipeline = Pipeline.Builder.create(this, "CareDocWebFrontendPipeline")
                .pipelineName("CareDocWebFrontendPipeline")
                // V2 を明示指定。V1 は未指定時の暗黙デフォルト（警告の原因）。
                // V2 は実行時間ベースの課金（無料枠あり）で、実行頻度の低い
                // 本用途では V1 の月額固定課金より安く済む。
                .pipelineType(PipelineType.V2)
                .build();

        pipeline.addStage(StageOptions.builder()
                .stageName("Source")
                .actions(List.of(
                        CodeStarConnectionsSourceAction.Builder.create()
                                .actionName("GitHubSource")
                                .owner("masalog")
                                .repo("CareDocWeb")
                                .branch("main")
                                .connectionArn(gitHubConnection.getAttrConnectionArn())
                                .output(sourceOutput)
                                .build()))
                .build());

        pipeline.addStage(StageOptions.builder()
                .stageName("Build")
                .actions(List.of(
                        CodeBuildAction.Builder.create()
                                .actionName("BuildFrontend")
                                .project(codeBuildProject)
                                .input(sourceOutput)
                                .build()))
                .build());

        CfnOutput.Builder.create(this, "CodeBuildProjectName")
                .value(codeBuildProject.getProjectName())
                .build();
    }
}