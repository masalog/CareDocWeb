package com.example.CareDocWeb.service;

import com.example.CareDocWeb.dto.PdfGenerateRequest;
import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PdfServiceImpl} のユニットテスト。
 *
 * <p>正常系・境界値・異常系を網羅し、PDFバイナリが正しく生成されることを検証する。
 * 実際のテンプレートPDF・フォント・座標YAMLを使用した結合テスト。</p>
 */
@DisplayName("PdfServiceImpl")
class PdfServiceImplTest {

    private PdfServiceImpl pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfServiceImpl();
    }

    private Member createFullMember() {
        Member member = new Member();
        member.setId(UUID.randomUUID());
        member.setInsuranceIdNumber("1234567890");
        member.setName("田中太郎");
        member.setFurigana("タナカタロウ");
        member.setBirthYear(1950);
        member.setBirthMonth(3);
        member.setBirthDay(15);
        member.setGender("男");
        member.setAddress("東京都新宿区1-2-3");
        member.setPhone("03-1234-5678");
        member.setCareLevel("要介護3");
        member.setStartYear(2025);
        member.setStartMonth(4);
        member.setStartDay(1);
        member.setEndYear(2026);
        member.setEndMonth(3);
        member.setEndDay(31);
        member.setInstitutionYear(2024);
        member.setInstitutionMonth(10);
        member.setInstitutionDay(1);
        return member;
    }

    private CommonSettings createFullSettings() {
        CommonSettings settings = new CommonSettings();
        settings.setId(UUID.randomUUID());
        settings.setSurveyAddress("東京都千代田区○○1-1");
        settings.setSurveyPhone("03-9999-0000");
        settings.setFacilityName("○○介護施設");
        settings.setFacilityPhone("03-8888-0000");
        settings.setInstitutionName("○○病院");
        settings.setInstitutionAddress("東京都港区△△2-2");
        settings.setAgentName("佐藤花子");
        settings.setAgentPostal("100-0001");
        settings.setAgentAddress("東京都千代田区□□3-3");
        settings.setAgentPhone("03-7777-0000");
        settings.setDoctorName("鈴木一郎");
        settings.setClinicName("鈴木クリニック");
        settings.setClinicPostal("160-0001");
        settings.setClinicAddress("東京都新宿区△△4-4");
        settings.setClinicPhone("03-6666-0000");
        return settings;
    }

    private PdfGenerateRequest createFullRequest() {
        return new PdfGenerateRequest(
                UUID.randomUUID(), 2026, 7, 2, "状態悪化のため"
        );
    }

    // ========================================
    // 正常系
    // ========================================

    @Nested
    @DisplayName("正常系")
    class Normal {

        @Test
        @DisplayName("全フィールドありでPDFバイナリが生成される")
        void generatesPdf_withAllFields() {
            // 準備
            Member member = createFullMember();
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
            // PDFヘッダー確認（%PDF-）
            assertEquals('%', (char) result[0]);
            assertEquals('P', (char) result[1]);
            assertEquals('D', (char) result[2]);
            assertEquals('F', (char) result[3]);
        }

        @Test
        @DisplayName("変更理由ありでPDFが生成される")
        void generatesPdf_withChangeReason() {
            // 準備
            Member member = createFullMember();
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = new PdfGenerateRequest(
                    UUID.randomUUID(), 2026, 7, 2, "状態悪化のため"
            );

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("変更理由なし（null）でもPDFが生成される")
        void generatesPdf_withoutChangeReason() {
            // 準備
            Member member = createFullMember();
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = new PdfGenerateRequest(
                    UUID.randomUUID(), 2026, 7, 2, null
            );

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("要支援レベルの利用者でもPDFが生成される")
        void generatesPdf_withSupportLevel() {
            // 準備
            Member member = createFullMember();
            member.setCareLevel("要支援1");
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("要支援2の利用者でもPDFが生成される")
        void generatesPdf_withSupportLevel2() {
            // 準備
            Member member = createFullMember();
            member.setCareLevel("要支援2");
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("女性の利用者でもPDFが生成される")
        void generatesPdf_withFemaleGender() {
            // 準備
            Member member = createFullMember();
            member.setGender("女");
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("全介護レベル（要介護1〜5）でPDFが生成される")
        void generatesPdf_withAllCareLevels() {
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            for (int i = 1; i <= 5; i++) {
                Member member = createFullMember();
                member.setCareLevel("要介護" + i);

                byte[] result = pdfService.generate(member, settings, request);

                assertNotNull(result, "要介護" + i + " でPDFが生成されない");
                assertTrue(result.length > 0);
            }
        }
    }

    // ========================================
    // 境界値
    // ========================================

    @Nested
    @DisplayName("境界値")
    class Boundary {

        @Test
        @DisplayName("利用者の任意フィールドがすべてNULLでもPDFが生成される")
        void generatesPdf_withMinimalMember() {
            // 準備（名前のみ必須）
            Member member = new Member();
            member.setId(UUID.randomUUID());
            member.setName("最小テスト");

            CommonSettings settings = new CommonSettings();
            settings.setId(UUID.randomUUID());

            PdfGenerateRequest request = new PdfGenerateRequest(
                    UUID.randomUUID(), 2026, 1, 1, null
            );

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("共通設定の全フィールドがNULLでもPDFが生成される")
        void generatesPdf_withEmptySettings() {
            // 準備
            Member member = createFullMember();
            CommonSettings settings = new CommonSettings();
            settings.setId(UUID.randomUUID());
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("介護度が未設定（null）でもPDFが生成される")
        void generatesPdf_withNullCareLevel() {
            // 準備
            Member member = createFullMember();
            member.setCareLevel(null);
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("介護度が不明な値でもPDFが生成される（〇は描画されない）")
        void generatesPdf_withUnknownCareLevel() {
            // 準備
            Member member = createFullMember();
            member.setCareLevel("不明");
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("性別が未設定（null）でもPDFが生成される")
        void generatesPdf_withNullGender() {
            // 準備
            Member member = createFullMember();
            member.setGender(null);
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("住所に改行が含まれていてもPDFが生成される")
        void generatesPdf_withMultiLineAddress() {
            // 準備
            Member member = createFullMember();
            member.setAddress("東京都中央区\n日本橋1-1-1\nマンション101");
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("変更理由が空文字でもPDFが生成される")
        void generatesPdf_withEmptyChangeReason() {
            // 準備
            Member member = createFullMember();
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = new PdfGenerateRequest(
                    UUID.randomUUID(), 2026, 7, 2, ""
            );

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("認定終了日がNULLでもPDFが生成される")
        void generatesPdf_withNullEndDate() {
            // 準備
            Member member = createFullMember();
            member.setEndYear(null);
            member.setEndMonth(null);
            member.setEndDay(null);
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("施設入所日がNULLでもPDFが生成される")
        void generatesPdf_withNullInstitutionDate() {
            // 準備
            Member member = createFullMember();
            member.setInstitutionYear(null);
            member.setInstitutionMonth(null);
            member.setInstitutionDay(null);
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行
            byte[] result = pdfService.generate(member, settings, request);

            // 検証
            assertNotNull(result);
            assertTrue(result.length > 0);
        }
    }

    // ========================================
    // 異常系
    // ========================================

    @Nested
    @DisplayName("異常系")
    class Error {

        @Test
        @DisplayName("利用者がNULLの場合、NullPointerExceptionがスローされる")
        void throwsException_whenMemberIsNull() {
            // 準備
            CommonSettings settings = createFullSettings();
            PdfGenerateRequest request = createFullRequest();

            // 実行 & 検証
            assertThrows(NullPointerException.class,
                    () -> pdfService.generate(null, settings, request));
        }

        @Test
        @DisplayName("共通設定がNULLの場合、NullPointerExceptionがスローされる")
        void throwsException_whenSettingsIsNull() {
            // 準備
            Member member = createFullMember();
            PdfGenerateRequest request = createFullRequest();

            // 実行 & 検証
            assertThrows(NullPointerException.class,
                    () -> pdfService.generate(member, null, request));
        }

        @Test
        @DisplayName("リクエストがNULLの場合、NullPointerExceptionがスローされる")
        void throwsException_whenRequestIsNull() {
            // 準備
            Member member = createFullMember();
            CommonSettings settings = createFullSettings();

            // 実行 & 検証
            assertThrows(NullPointerException.class,
                    () -> pdfService.generate(member, settings, null));
        }
    }
}
