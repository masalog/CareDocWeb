package com.example.CareDocWeb.controller;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link HealthController} の統合テスト。
 *
 * <p>{@code @WebMvcTest} を使用し、HTTPリクエスト/レスポンスの観点から
 * ヘルスチェックの動作を検証する。DataSource はモック化する。</p>
 *
 * <p>ヘルスチェックはウォームアップ用途のため、DB接続の成否にかかわらず
 * 常に 200 を返し、db フィールドで接続状態（up / down）を示すことを確認する。</p>
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSource dataSource;

    // ========================================
    // GET /api/health
    // ========================================

    @Nested
    @DisplayName("GET /api/health")
    class Health {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: DB接続が有効な場合、200で status=ok, db=up を返す")
        void returns200_withDbUp() throws Exception {
            // 準備
            Connection connection = mock(Connection.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(anyInt())).thenReturn(true);

            // 実行 & 検証
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("ok"))
                    .andExpect(jsonPath("$.db").value("up"));

            verify(dataSource, times(1)).getConnection();
            verify(connection, times(1)).isValid(anyInt());
            // try-with-resources で接続がクローズされることを確認
            verify(connection, times(1)).close();
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: DB接続が無効(isValid=false)の場合、200で status=ok, db=down を返す")
        void returns200_withDbDown_whenNotValid() throws Exception {
            // 準備
            Connection connection = mock(Connection.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(anyInt())).thenReturn(false);

            // 実行 & 検証
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"))
                    .andExpect(jsonPath("$.db").value("down"));
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: DB接続取得で例外が発生しても、200で status=ok, db=down を返す")
        void returns200_withDbDown_whenConnectionThrows() throws Exception {
            // 準備: 接続取得自体が失敗（Supabase 一時障害などを想定）
            when(dataSource.getConnection())
                    .thenThrow(new java.sql.SQLException("DB接続エラー"));

            // 実行 & 検証: ウォームアップ用途のためエンドポイント自体は 200 を維持する
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ok"))
                    .andExpect(jsonPath("$.db").value("down"));
        }
    }
}
