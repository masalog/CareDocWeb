package com.example.cdk;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.constructs.Construct;
import software.amazon.awscdk.services.cognito.PasswordPolicy;

/**
 * 管理画面用の Cognito 認証基盤。
 * セルフサインアップは無効化し、管理者はコンソール/CLI から手動追加する。
 */
public class AuthConstruct extends Construct {

    private final UserPool userPool;
    private final UserPoolClient userPoolClient;

    public AuthConstruct(final Construct scope, final String id) {
        super(scope, id);

        this.userPool = UserPool.Builder.create(this, "AdminUserPool")
                .userPoolName("caredocweb-admin")
                .selfSignUpEnabled(false)
                .signInAliases(SignInAliases.builder().email(true).build())
                .passwordPolicy(PasswordPolicy.builder()
                        .minLength(8)
                        .requireLowercase(true)
                        .requireUppercase(true)
                        .requireDigits(true)
                        .requireSymbols(false)　// 記号は不要とする
                        .build())
                .build();

        // スタック削除時もユーザー情報を残す(誤削除対策)
        userPool.applyRemovalPolicy(RemovalPolicy.RETAIN);

        this.userPoolClient = UserPoolClient.Builder.create(this, "AdminClient")
                .userPool(userPool)
                .authFlows(AuthFlow.builder().userSrp(true).build())
                .generateSecret(false)  // ブラウザ SPA なのでシークレットなし
                .build();
    }

    public UserPool getUserPool() {
        return userPool;
    }

    public UserPoolClient getUserPoolClient() {
        return userPoolClient;
    }
}