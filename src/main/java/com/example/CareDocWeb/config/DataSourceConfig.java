package com.example.CareDocWeb.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * 本番（prod）用の DataSource を組み立てる設定クラス。
 *
 * <p>方式B（完全版）：DB の接続情報（URL・ユーザー名・パスワード）を
 * 環境変数に実値で置かず、Lambda 実行時に SSM Parameter Store から取得する。
 * 環境変数には「パラメータ名」のみを渡し、実値は一切焼き込まない。</p>
 *
 * <p>環境変数（いずれも SSM のパラメータ名）：</p>
 * <ul>
 *   <li>DB_URL_PARAM … JDBC URL の SSM パラメータ名</li>
 *   <li>DB_USERNAME_PARAM … DB ユーザー名の SSM パラメータ名</li>
 *   <li>DB_PASSWORD_PARAM … DB パスワード（SecureString）の SSM パラメータ名</li>
 * </ul>
 *
 * <p>prod プロファイルのときだけ有効。local / test では従来どおり
 * application.yaml の設定で DataSource が自動構築される。</p>
 */
@Configuration
@Profile("prod")
public class DataSourceConfig {

    /**
     * SSM から接続情報をすべて取得して本番用 DataSource を構築する。
     *
     * @param urlParam      JDBC URL の SSM パラメータ名（環境変数 DB_URL_PARAM）
     * @param usernameParam DB ユーザー名の SSM パラメータ名（環境変数 DB_USERNAME_PARAM）
     * @param secretParam   DB パスワードの SSM パラメータ名（環境変数 DB_PASSWORD_PARAM）
     * @return 構築済みの DataSource
     */
    @Bean
    public DataSource dataSource(
            @Value("${DB_URL_PARAM}") String urlParam,
            @Value("${DB_USERNAME_PARAM}") String usernameParam,
            @Value("${DB_PASSWORD_PARAM}") String secretParam) {

        try (SsmClient ssm = SsmClient.create()) {
            return DataSourceBuilder.create()
                    .url(fetchParameter(ssm, urlParam))
                    .username(fetchParameter(ssm, usernameParam))
                    .password(fetchParameter(ssm, secretParam))
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }
    }

    /**
     * SSM Parameter Store からパラメータ値を取得する（SecureString は自動復号）。
     * リージョンや認証情報は Lambda 実行環境（デフォルトチェーン）から自動解決される。
     *
     * @param ssm           SSM クライアント
     * @param parameterName SSM パラメータ名（例: /caredocweb/db-url）
     * @return （必要に応じて復号済みの）パラメータ値
     */
    private String fetchParameter(SsmClient ssm, String parameterName) {
        GetParameterRequest request = GetParameterRequest.builder()
                .name(parameterName)
                // SecureString の場合は復号して取得（String パラメータには無害）
                .withDecryption(true)
                .build();
        GetParameterResponse response = ssm.getParameter(request);
        return response.parameter().value();
    }
}
