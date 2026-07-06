package com.example.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.ApiKey;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.Period;
import software.amazon.awscdk.services.apigateway.QuotaSettings;
import software.amazon.awscdk.services.apigateway.ThrottleSettings;
import software.amazon.awscdk.services.apigateway.UsagePlan;
import software.amazon.awscdk.services.apigateway.UsagePlanPerApiStage;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.SnapStartConf;
import software.amazon.awscdk.services.lambda.Version;
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.SecureStringParameterAttributes;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

/**
 * バックエンド（Lambda + API Gateway）用スタック。
 *
 * 構成：
 *   API Gateway（REST API）─→ Lambda（Spring Boot + SnapStart）─→ Supabase
 *
 * API Gateway は REST API（LambdaRestApi）を使用。aws-cdk-lib 本体に含まれる
 * 安定 API で、全パスを Lambda にプロキシ統合する。
 *
 * デモ公開向けに、使用量プラン（レート制限・日次クォータ・API キー）を設定し、
 * 不正な大量リクエストによるコスト増を防止する。
 *
 * DB 接続情報（URL・ユーザー名・パスワード）はすべて方式Bで統一し、
 * Lambda 実行時にアプリが SSM から取得する。環境変数にはパラメータ名のみを渡す。
 */
public class BackendStack extends Stack {

    // FrontendStack から CloudFront の /api/* オリジンとして参照するため公開する。
    private final LambdaRestApi api;

    public BackendStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // ------------------------------------------------------------
        // 1. SSM パラメータ名（実値ではなく参照だけを扱う）
        // ------------------------------------------------------------
        String dbUrlParamName = "/caredocweb/db-url";
        String dbUsernameParamName = "/caredocweb/db-username";
        String dbPasswordParamName = "/caredocweb/db-password";

        // ------------------------------------------------------------
        // 2. Lambda 関数（Spring Boot + SnapStart）
        // ------------------------------------------------------------
        Function apiFunction = Function.Builder.create(this, "ApiFunction")
                .runtime(Runtime.JAVA_21)
                .code(Code.fromAsset("../target/CareDocWeb-0.0.1-SNAPSHOT.jar"))
                .handler("com.example.CareDocWeb.StreamLambdaHandler::handleRequest")
                // メモリは 2048MB（エイリアス化のみでコールド約1.1秒を達成した最良構成）。
                // 3008MB は SnapStart の復元コストが増えて逆効果、1024MB は CPU 減の懸念があり、
                // 実測で最も安定して速かった 2048MB を採用する。
                // （CloudWatch 実測の Max Memory Used は 379MB で使用量自体には余裕がある）
                .memorySize(2048)
                .timeout(Duration.seconds(30))
                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
                .environment(Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",
                        "DB_URL_PARAM", dbUrlParamName,
                        "DB_USERNAME_PARAM", dbUsernameParamName,
                        "DB_PASSWORD_PARAM", dbPasswordParamName))
                .build();

        // ------------------------------------------------------------
        // 3. Lambda に SSM パラメータの読み取り権限を付与（3つとも）
        // ------------------------------------------------------------
        IStringParameter urlParam = StringParameter.fromStringParameterName(
                this, "DbUrlParam", dbUrlParamName);
        IStringParameter usernameParam = StringParameter.fromStringParameterName(
                this, "DbUsernameParam", dbUsernameParamName);
        IStringParameter passwordParam = StringParameter.fromSecureStringParameterAttributes(
                this, "DbPasswordParam",
                SecureStringParameterAttributes.builder()
                        .parameterName(dbPasswordParamName)
                        .build());

        for (IStringParameter p : List.of(urlParam, usernameParam, passwordParam)) {
            p.grantRead(apiFunction);
        }

        // ------------------------------------------------------------
        // 4. 公開バージョン + エイリアス（SnapStart を実際に効かせる）
        //    SnapStart は「公開バージョン」のスナップショットにのみ効く。
        //    Function を直接 API Gateway に渡すと $LATEST が呼ばれ、
        //    SnapStartConf.ON_PUBLISHED_VERSIONS を設定しても効かない。
        //    そこで getCurrentVersion() で公開バージョンを発行し、
        //    エイリアス "live" 経由で呼ぶことで SnapStart を有効化する。
        // ------------------------------------------------------------
        Version apiVersion = apiFunction.getCurrentVersion();
        Alias apiAlias = Alias.Builder.create(this, "ApiAlias")
                .aliasName("live")
                .version(apiVersion)
                .build();

        // ------------------------------------------------------------
        // 5. API Gateway（REST API）
        //    全パスを Lambda にプロキシ統合する
        //    handler にはエイリアスを渡す（$LATEST ではなく SnapStart 対象バージョン）
        // ------------------------------------------------------------
        this.api = LambdaRestApi.Builder.create(this, "RestApi")
                .handler(apiAlias)
                .restApiName("CareDocWeb API")
                // PDF などのバイナリレスポンスを正しく返すために必要。
                // serverless-java-container はバイナリを Base64 で返すため、
                // API Gateway 側で binaryMediaTypes を設定しないと
                // Base64 文字列のままクライアントに届き、PDF が開けなくなる。
                // "*/*" を指定すると、クライアントの Accept ヘッダーに応じて
                // API Gateway が Base64 を自動デコードしてバイナリを返す。
                .binaryMediaTypes(List.of("*/*"))
                .build();

        // ------------------------------------------------------------
        // 6. デモ用の使用量プラン（レート制限・日次クォータ・API キー）
        //    不正な大量アクセスによるコスト増を防止する
        // ------------------------------------------------------------
        // API キーを発行（フロントエンドが x-api-key ヘッダーで送信する想定）
        ApiKey apiKey = ApiKey.Builder.create(this, "DemoApiKey")
                .apiKeyName("caredocweb-demo-key")
                .build();

        // 使用量プラン：秒間レート・バースト・日次クォータを設定
        UsagePlan usagePlan = UsagePlan.Builder.create(this, "DemoUsagePlan")
                .name("caredocweb-demo-plan")
                .throttle(ThrottleSettings.builder()
                        .rateLimit(10)   // 秒間 10 リクエスト
                        .burstLimit(20)  // バースト 20 リクエスト
                        .build())
                .quota(QuotaSettings.builder()
                        .limit(1000)          // 1 日 1000 リクエストまで
                        .period(Period.DAY)
                        .build())
                .apiStages(List.of(UsagePlanPerApiStage.builder()
                        .api(api)
                        .stage(api.getDeploymentStage())
                        .build()))
                .build();

        // API キーを使用量プランに紐付け
        usagePlan.addApiKey(apiKey);

        // ------------------------------------------------------------
        // 7. 出力
        // ------------------------------------------------------------
        CfnOutput.Builder.create(this, "ApiUrl")
                .description("API Gateway のエンドポイント URL")
                .value(api.getUrl())
                .exportName("CareDocWebApiUrl")
                .build();

        CfnOutput.Builder.create(this, "ApiKeyId")
                .description("API キーの ID（値は AWS コンソール/CLI で取得）")
                .value(apiKey.getKeyId())
                .build();
    }

    /** FrontendStack が CloudFront の /api/* オリジンとして参照するための API Gateway を返す。 */
    public LambdaRestApi getApi() {
        return this.api;
    }
}
