package com.example.CareDocWeb.controller;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ヘルスチェック用エンドポイント。
 *
 * <p>用途：</p>
 * <ul>
 *   <li>死活監視（アプリが起動しているかの確認）</li>
 *   <li>定期ウォームアップ（外部cronから定期的に叩き、Lambda をアイドルにさせない）</li>
 * </ul>
 *
 * <p>DB 接続を軽く確認（Connection を取得するだけ）するため、
 * ウォームアップ時にコネクションプールも温まる。重いクエリは投げない。</p>
 *
 * <p>/api/health は /api/* ビヘイビアに含まれ CloudFront ではキャッシュ無効
 * （CACHING_DISABLED）のため、外部から叩くと毎回 Lambda まで到達する。</p>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * ヘルスチェック。DB 接続を軽く確認して結果を返す。
     * DB 接続に失敗しても 200 を返し、status で db の状態を示す
     * （ウォームアップ目的のため、DB 一時障害でエンドポイント自体は生かす）。
     *
     * @return status（ok）と db（up / down）を含む JSON
     */
    @GetMapping
    public Map<String, String> health() {
        String dbStatus;
        try (var connection = dataSource.getConnection()) {
            // 接続の有効性を軽く確認（タイムアウト2秒）
            dbStatus = connection.isValid(2) ? "up" : "down";
        } catch (Exception e) {
            dbStatus = "down";
        }
        return Map.of(
                "status", "ok",
                "db", dbStatus);
    }
}
