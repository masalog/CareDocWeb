package com.example.CareDocWeb.service;

import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.exception.ResourceNotFoundException;
import com.example.CareDocWeb.repository.CommonSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link CommonSettingsServiceImpl} のユニットテスト。
 *
 * <p>全メソッドに対して正常系・境界値・異常系を網羅的にカバーする。
 * Repositoryをモック化し、サービス層のロジックのみを検証する。</p>
 *
 * <p>{@code common_settings} テーブルは常に1レコードのみ存在する前提のため、
 * 0件・1件・複数件のケースを中心にテストする。</p>
 */
@ExtendWith(MockitoExtension.class)
class CommonSettingsServiceImplTest {

    @Mock
    private CommonSettingsRepository commonSettingsRepository;

    @InjectMocks
    private CommonSettingsServiceImpl commonSettingsService;

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
    // find
    // ========================================

    @Nested
    @DisplayName("find")
    class Find {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 共通設定が1件存在する場合、設定値を返す")
        void returnsSettings_whenOneRecordExists() {
            // 準備
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));

            // 実行
            CommonSettings result = commonSettingsService.find();

            // 検証
            assertNotNull(result);
            assertEquals(sampleId, result.getId());
            verify(commonSettingsRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("正常系: 全フィールドが正しく取得できる")
        void returnsAllFieldsCorrectly() {
            // 準備
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));

            // 実行
            CommonSettings result = commonSettingsService.find();

            // 検証
            assertEquals("東京都中央区日本橋3-3-3", result.getSurveyAddress());
            assertEquals("03-1111-2222", result.getSurveyPhone());
            assertEquals("中央介護センター", result.getFacilityName());
            assertEquals("03-3333-4444", result.getFacilityPhone());
            assertEquals("中央病院", result.getInstitutionName());
            assertEquals("東京都中央区銀座4-4-4", result.getInstitutionAddress());
            assertEquals("山田一郎", result.getAgentName());
            assertEquals("104-0061", result.getAgentPostal());
            assertEquals("東京都中央区銀座5-5-5", result.getAgentAddress());
            assertEquals("03-5555-6666", result.getAgentPhone());
            assertEquals("佐藤医師", result.getDoctorName());
            assertEquals("中央クリニック", result.getClinicName());
            assertEquals("104-0062", result.getClinicPostal());
            assertEquals("東京都中央区銀座6-6-6", result.getClinicAddress());
            assertEquals("03-7777-8888", result.getClinicPhone());
        }

        @Test
        @DisplayName("正常系: リポジトリのfindAllが1回だけ呼ばれる")
        void callsRepositoryExactlyOnce() {
            // 準備
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));

            // 実行
            commonSettingsService.find();

            // 検証
            verify(commonSettingsRepository, times(1)).findAll();
            verifyNoMoreInteractions(commonSettingsRepository);
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 複数レコードが存在しても先頭の1件のみ返す")
        void returnsFirstRecord_whenMultipleExist() {
            // 準備
            CommonSettings secondSettings = new CommonSettings();
            secondSettings.setId(UUID.randomUUID());
            secondSettings.setFacilityName("別の施設");
            when(commonSettingsRepository.findAll())
                    .thenReturn(List.of(sampleSettings, secondSettings));

            // 実行
            CommonSettings result = commonSettingsService.find();

            // 検証
            assertEquals(sampleId, result.getId());
            assertEquals("中央介護センター", result.getFacilityName());
        }

        @Test
        @DisplayName("境界値: 一部フィールドがNULLの設定でも取得できる")
        void returnsSettings_withNullFields() {
            // 準備
            sampleSettings.setClinicName(null);
            sampleSettings.setClinicPostal(null);
            sampleSettings.setClinicAddress(null);
            sampleSettings.setClinicPhone(null);
            sampleSettings.setInstitutionName(null);
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));

            // 実行
            CommonSettings result = commonSettingsService.find();

            // 検証
            assertNotNull(result);
            assertEquals("中央介護センター", result.getFacilityName());
            assertNull(result.getClinicName());
            assertNull(result.getClinicPostal());
            assertNull(result.getClinicAddress());
            assertNull(result.getClinicPhone());
            assertNull(result.getInstitutionName());
        }

        @Test
        @DisplayName("境界値: 全フィールドが空文字の設定でも取得できる")
        void returnsSettings_withEmptyStrings() {
            // 準備
            CommonSettings emptySettings = new CommonSettings();
            emptySettings.setId(UUID.randomUUID());
            emptySettings.setSurveyAddress("");
            emptySettings.setSurveyPhone("");
            emptySettings.setFacilityName("");
            emptySettings.setFacilityPhone("");
            when(commonSettingsRepository.findAll()).thenReturn(List.of(emptySettings));

            // 実行
            CommonSettings result = commonSettingsService.find();

            // 検証
            assertNotNull(result);
            assertEquals("", result.getSurveyAddress());
            assertEquals("", result.getFacilityName());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: 共通設定が0件の場合、RuntimeExceptionをスローする")
        void throwsException_whenNoRecords() {
            // 準備
            when(commonSettingsRepository.findAll()).thenReturn(Collections.emptyList());

            // 実行 & 検証
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> commonSettingsService.find());
            assertTrue(exception.getMessage().contains("共通設定が登録されていません"));
            verify(commonSettingsRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void propagatesException_whenRepositoryThrows() {
            // 準備
            when(commonSettingsRepository.findAll())
                    .thenThrow(new RuntimeException("DB接続エラー"));

            // 実行 & 検証
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> commonSettingsService.find());
            assertEquals("DB接続エラー", exception.getMessage());
        }
    }

    // ========================================
    // save
    // ========================================

    @Nested
    @DisplayName("save")
    class Save {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 共通設定を更新して保存後のエンティティを返す")
        void savesAndReturnsUpdatedEntity() {
            // 準備
            sampleSettings.setFacilityName("新しい施設名");
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));
            when(commonSettingsRepository.save(sampleSettings)).thenReturn(sampleSettings);

            // 実行
            CommonSettings result = commonSettingsService.save(sampleSettings);

            // 検証
            assertEquals(sampleId, result.getId());
            assertEquals("新しい施設名", result.getFacilityName());
            verify(commonSettingsRepository, times(1)).save(sampleSettings);
        }

        @Test
        @DisplayName("正常系: 複数フィールドを同時に更新する")
        void updatesMultipleFields() {
            // 準備
            sampleSettings.setSurveyAddress("新住所");
            sampleSettings.setSurveyPhone("03-9999-0000");
            sampleSettings.setDoctorName("新しい医師");
            sampleSettings.setClinicName("新クリニック");
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));
            when(commonSettingsRepository.save(sampleSettings)).thenReturn(sampleSettings);

            // 実行
            CommonSettings result = commonSettingsService.save(sampleSettings);

            // 検証
            assertEquals("新住所", result.getSurveyAddress());
            assertEquals("03-9999-0000", result.getSurveyPhone());
            assertEquals("新しい医師", result.getDoctorName());
            assertEquals("新クリニック", result.getClinicName());
        }

        @Test
        @DisplayName("正常系: リポジトリのsaveが1回だけ呼ばれる")
        void callsRepositoryExactlyOnce() {
            // 準備
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));
            when(commonSettingsRepository.save(sampleSettings)).thenReturn(sampleSettings);

            // 実行
            commonSettingsService.save(sampleSettings);

            // 検証
            verify(commonSettingsRepository, times(1)).save(sampleSettings);
            verify(commonSettingsRepository, times(1)).findAll();
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 一部フィールドをNULLに更新しても保存できる")
        void savesWithNullFields() {
            // 準備
            sampleSettings.setClinicName(null);
            sampleSettings.setClinicPostal(null);
            sampleSettings.setClinicAddress(null);
            sampleSettings.setClinicPhone(null);
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));
            when(commonSettingsRepository.save(sampleSettings)).thenReturn(sampleSettings);

            // 実行
            CommonSettings result = commonSettingsService.save(sampleSettings);

            // 検証
            assertNotNull(result.getId());
            assertNull(result.getClinicName());
            assertNull(result.getClinicPostal());
            assertNull(result.getClinicAddress());
            assertNull(result.getClinicPhone());
            verify(commonSettingsRepository, times(1)).save(sampleSettings);
        }

        @Test
        @DisplayName("境界値: 全フィールドを空文字に更新しても保存できる")
        void savesWithEmptyStrings() {
            // 準備
            sampleSettings.setSurveyAddress("");
            sampleSettings.setSurveyPhone("");
            sampleSettings.setFacilityName("");
            sampleSettings.setFacilityPhone("");
            sampleSettings.setAgentName("");
            sampleSettings.setDoctorName("");
            sampleSettings.setClinicName("");
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));
            when(commonSettingsRepository.save(sampleSettings)).thenReturn(sampleSettings);

            // 実行
            CommonSettings result = commonSettingsService.save(sampleSettings);

            // 検証
            assertEquals("", result.getSurveyAddress());
            assertEquals("", result.getFacilityName());
            assertEquals("", result.getAgentName());
            assertEquals("", result.getDoctorName());
        }

        @Test
        @DisplayName("境界値: 住所が非常に長い文字列でも保存できる")
        void savesWithVeryLongString() {
            // 準備
            String longAddress = "あ".repeat(500);
            sampleSettings.setSurveyAddress(longAddress);
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));
            when(commonSettingsRepository.save(sampleSettings)).thenReturn(sampleSettings);

            // 実行
            CommonSettings result = commonSettingsService.save(sampleSettings);

            // 検証
            assertEquals(500, result.getSurveyAddress().length());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: NULLのエンティティを渡した場合、リポジトリに委譲される")
        void throwsException_whenEntityIsNull() {
            // 準備
            when(commonSettingsRepository.findAll()).thenReturn(Collections.emptyList());
            when(commonSettingsRepository.save(null))
                    .thenThrow(new IllegalArgumentException("エンティティがnullです"));

            // 実行 & 検証
            assertThrows(IllegalArgumentException.class,
                    () -> commonSettingsService.save(null));
            verify(commonSettingsRepository, times(1)).save(null);
        }

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void propagatesException_whenRepositoryThrows() {
            // 準備
            when(commonSettingsRepository.findAll()).thenReturn(List.of(sampleSettings));
            when(commonSettingsRepository.save(sampleSettings))
                    .thenThrow(new RuntimeException("DB書き込みエラー"));

            // 実行 & 検証
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> commonSettingsService.save(sampleSettings));
            assertEquals("DB書き込みエラー", exception.getMessage());
        }
    }
}
