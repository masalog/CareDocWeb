package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.service.CommonSettingsService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link CommonSettingsController} の統合テスト。
 *
 * <p>{@code @WebMvcTest} を使用し、HTTPリクエスト/レスポンスの観点から
 * コントローラー層の動作を検証する。サービス層はモック化する。</p>
 */
@WebMvcTest(CommonSettingsController.class)
class CommonSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommonSettingsService commonSettingsService;

    private CommonSettings sampleSettings;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleSettings = new CommonSettings();
        sampleSettings.setId(sampleId);
        sampleSettings.setSurveyAddress("東京都中央区日本橋3-3-3");
        sampleSettings.setSurveyPhone("03-1111-2222");
        sampleSettings.setFacilityName("中央介護センター");
        sampleSettings.setFacilityPhone("03-3333-4444");
        sampleSettings.setInstitutionName("中央病院");
        sampleSettings.setInstitutionAddress("東京都中央区銀座4-4-4");
        sampleSettings.setInstitutionYear(2026);
        sampleSettings.setInstitutionMonth(3);
        sampleSettings.setInstitutionDay(15);
        sampleSettings.setAgentName("山田一郎");
        sampleSettings.setAgentPostal("104-0061");
        sampleSettings.setAgentAddress("東京都中央区銀座5-5-5");
        sampleSettings.setAgentPhone("03-5555-6666");
        sampleSettings.setDoctorName("佐藤医師");
        sampleSettings.setClinicName("中央クリニック");
        sampleSettings.setClinicPostal("104-0062");
        sampleSettings.setClinicAddress("東京都中央区銀座6-6-6");
        sampleSettings.setClinicPhone("03-7777-8888");
    }

    // ========================================
    // GET /api/settings
    // ========================================

    @Nested
    @DisplayName("GET /api/settings")
    class Find {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 共通設定を200で返す")
        void returns200_withSettings() throws Exception {
            // 準備
            when(commonSettingsService.find()).thenReturn(sampleSettings);

            // 実行 & 検証
            mockMvc.perform(get("/api/settings"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(sampleId.toString()))
                    .andExpect(jsonPath("$.facilityName").value("中央介護センター"))
                    .andExpect(jsonPath("$.agentName").value("山田一郎"))
                    .andExpect(jsonPath("$.doctorName").value("佐藤医師"))
                    .andExpect(jsonPath("$.clinicName").value("中央クリニック"));

            verify(commonSettingsService, times(1)).find();
        }

        @Test
        @DisplayName("正常系: 全フィールドが正しくJSON出力される")
        void returnsAllFields() throws Exception {
            // 準備
            when(commonSettingsService.find()).thenReturn(sampleSettings);

            // 実行 & 検証
            mockMvc.perform(get("/api/settings"))
                    .andExpect(jsonPath("$.surveyAddress").value("東京都中央区日本橋3-3-3"))
                    .andExpect(jsonPath("$.surveyPhone").value("03-1111-2222"))
                    .andExpect(jsonPath("$.facilityName").value("中央介護センター"))
                    .andExpect(jsonPath("$.facilityPhone").value("03-3333-4444"))
                    .andExpect(jsonPath("$.institutionName").value("中央病院"))
                    .andExpect(jsonPath("$.institutionAddress").value("東京都中央区銀座4-4-4"))
                    .andExpect(jsonPath("$.institutionYear").value(2026))
                    .andExpect(jsonPath("$.institutionMonth").value(3))
                    .andExpect(jsonPath("$.institutionDay").value(15))
                    .andExpect(jsonPath("$.agentName").value("山田一郎"))
                    .andExpect(jsonPath("$.agentPostal").value("104-0061"))
                    .andExpect(jsonPath("$.agentAddress").value("東京都中央区銀座5-5-5"))
                    .andExpect(jsonPath("$.agentPhone").value("03-5555-6666"))
                    .andExpect(jsonPath("$.doctorName").value("佐藤医師"))
                    .andExpect(jsonPath("$.clinicName").value("中央クリニック"))
                    .andExpect(jsonPath("$.clinicPostal").value("104-0062"))
                    .andExpect(jsonPath("$.clinicAddress").value("東京都中央区銀座6-6-6"))
                    .andExpect(jsonPath("$.clinicPhone").value("03-7777-8888"));
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 一部フィールドがNULLの場合でも200で返す")
        void returns200_withNullFields() throws Exception {
            // 準備
            sampleSettings.setClinicName(null);
            sampleSettings.setClinicPostal(null);
            when(commonSettingsService.find()).thenReturn(sampleSettings);

            // 実行 & 検証
            mockMvc.perform(get("/api/settings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.facilityName").value("中央介護センター"))
                    .andExpect(jsonPath("$.clinicName").isEmpty())
                    .andExpect(jsonPath("$.clinicPostal").isEmpty());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: 共通設定が未登録の場合、404を返す")
        void returns404_whenNotRegistered() throws Exception {
            // 準備
            when(commonSettingsService.find())
                    .thenThrow(new com.example.CareDocWeb.exception.ResourceNotFoundException("共通設定が登録されていません"));

            // 実行 & 検証
            mockMvc.perform(get("/api/settings"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("異常系: サービスが例外をスローした場合、500を返す")
        void returns500_whenServiceThrows() throws Exception {
            // 準備
            when(commonSettingsService.find())
                    .thenThrow(new RuntimeException("DB接続エラー"));

            // 実行 & 検証
            mockMvc.perform(get("/api/settings"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ========================================
    // PUT /api/settings
    // ========================================

    @Nested
    @DisplayName("PUT /api/settings")
    class Update {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 共通設定を更新して200で返す")
        void returns200_withUpdatedSettings() throws Exception {
            // 準備
            sampleSettings.setFacilityName("新しい施設名");
            when(commonSettingsService.save(any(CommonSettings.class))).thenReturn(sampleSettings);

            // 実行 & 検証
            mockMvc.perform(put("/api/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleSettings)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.facilityName").value("新しい施設名"));

            verify(commonSettingsService, times(1)).save(any(CommonSettings.class));
        }

        @Test
        @DisplayName("正常系: 複数フィールドを同時に更新できる")
        void returns200_withMultipleFieldsUpdated() throws Exception {
            // 準備
            sampleSettings.setDoctorName("新しい医師");
            sampleSettings.setClinicName("新クリニック");
            when(commonSettingsService.save(any(CommonSettings.class))).thenReturn(sampleSettings);

            // 実行 & 検証
            mockMvc.perform(put("/api/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleSettings)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.doctorName").value("新しい医師"))
                    .andExpect(jsonPath("$.clinicName").value("新クリニック"));
        }

        @Test
        @DisplayName("正常系: 入院・入所年月日を更新して200で返す")
        void returns200_withInstitutionDateUpdated() throws Exception {
            // 準備
            sampleSettings.setInstitutionYear(2025);
            sampleSettings.setInstitutionMonth(12);
            sampleSettings.setInstitutionDay(31);
            when(commonSettingsService.save(any(CommonSettings.class))).thenReturn(sampleSettings);

            // 実行 & 検証
            mockMvc.perform(put("/api/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleSettings)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.institutionYear").value(2025))
                    .andExpect(jsonPath("$.institutionMonth").value(12))
                    .andExpect(jsonPath("$.institutionDay").value(31));
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 入院・入所年月日をNULLに更新できる")
        void returns200_withInstitutionDateNull() throws Exception {
            // 準備
            sampleSettings.setInstitutionYear(null);
            sampleSettings.setInstitutionMonth(null);
            sampleSettings.setInstitutionDay(null);
            when(commonSettingsService.save(any(CommonSettings.class))).thenReturn(sampleSettings);

            // 実行 & 検証
            mockMvc.perform(put("/api/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleSettings)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.institutionYear").isEmpty())
                    .andExpect(jsonPath("$.institutionMonth").isEmpty())
                    .andExpect(jsonPath("$.institutionDay").isEmpty());
        }

        @Test
        @DisplayName("境界値: 一部フィールドをNULLに更新できる")
        void returns200_withNullFields() throws Exception {
            // 準備
            sampleSettings.setClinicName(null);
            sampleSettings.setClinicPhone(null);
            when(commonSettingsService.save(any(CommonSettings.class))).thenReturn(sampleSettings);

            // 実行 & 検証
            mockMvc.perform(put("/api/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleSettings)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clinicName").isEmpty())
                    .andExpect(jsonPath("$.clinicPhone").isEmpty());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: リクエストボディが空の場合、400を返す")
        void returns400_whenBodyIsEmpty() throws Exception {
            // 実行 & 検証
            mockMvc.perform(put("/api/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("異常系: サービスが例外をスローした場合、500を返す")
        void returns500_whenServiceThrows() throws Exception {
            // 準備
            when(commonSettingsService.save(any(CommonSettings.class)))
                    .thenThrow(new RuntimeException("DB書き込みエラー"));

            // 実行 & 検証
            mockMvc.perform(put("/api/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleSettings)))
                    .andExpect(status().isInternalServerError());
        }
    }
}
