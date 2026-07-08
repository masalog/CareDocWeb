package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.dto.PdfGenerateRequest;
import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.exception.ResourceNotFoundException;
import com.example.CareDocWeb.service.CommonSettingsService;
import com.example.CareDocWeb.service.MemberService;
import com.example.CareDocWeb.service.PdfService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.HttpHeaders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * {@link PdfController} の統合テスト。
 *
 * <p>{@code @WebMvcTest} を使用し、HTTPリクエスト/レスポンスの観点から
 * PDF生成エンドポイントの動作を検証する。</p>
 *
 * <p>テストは「正常系」「境界値」「異常系」の3分類で構成する。</p>
 */
@WebMvcTest(PdfController.class)
@DisplayName("POST /api/pdf/generate")
class PdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private CommonSettingsService commonSettingsService;

    @MockitoBean
    private PdfService pdfService;

    private final UUID sampleMemberId = UUID.fromString("d265e665-368a-44a9-9808-fe5c063bec44");

    // 申請年は「当年・翌年のみ許可」のバリデーションがあるため、
    // テストの申請年は固定値ではなく実行時の当年を使う（将来もテストが壊れない）。
    private final int validYear = LocalDate.now().getYear();

    private Member createSampleMember() {
        Member member = new Member();
        member.setId(sampleMemberId);
        member.setName("田中太郎");
        member.setFurigana("タナカタロウ");
        member.setCareLevel("要介護2");
        return member;
    }

    private CommonSettings createSampleSettings() {
        CommonSettings settings = new CommonSettings();
        settings.setId(UUID.randomUUID());
        settings.setFacilityName("中央介護センター");
        return settings;
    }

    // ========================================
    // 正常系
    // ========================================

    @Nested
    @DisplayName("正常系")
    class Normal {

        @Test
        @DisplayName("200を返しContent-TypeがPDFである")
        void returns200_withPdfContentType() throws Exception {

            // 準備
            Member member = createSampleMember();
            CommonSettings settings = createSampleSettings();
            byte[] fakePdf = "%PDF-1.4 fake content".getBytes();

            when(memberService.findById(sampleMemberId)).thenReturn(member);
            when(commonSettingsService.find()).thenReturn(settings);
            when(pdfService.generate(
                    any(Member.class),
                    any(CommonSettings.class),
                    any(PdfGenerateRequest.class)))
                    .thenReturn(fakePdf);

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId,
                    validYear,
                    7,
                    2,
                    "状態悪化のため"
            );

            String expectedFileName =
                    String.format(
                            "%04d年7月2日 田中太郎様 介護認定申請書.pdf",
                            validYear
                    );

            String encodedFileName =
                    URLEncoder.encode(
                            expectedFileName,
                            StandardCharsets.UTF_8
                    ).replace("+", "%20");

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName
                    ));

            verify(memberService, times(1)).findById(sampleMemberId);
            verify(commonSettingsService, times(1)).find();
            verify(pdfService, times(1)).generate(any(), any(), any());
        }

        @Test
        @DisplayName("変更理由なし（null）でも200を返す")
        void returns200_withoutChangeReason() throws Exception {
            // 準備
            Member member = createSampleMember();
            CommonSettings settings = createSampleSettings();
            byte[] fakePdf = "%PDF-1.4 fake".getBytes();

            when(memberService.findById(sampleMemberId)).thenReturn(member);
            when(commonSettingsService.find()).thenReturn(settings);
            when(pdfService.generate(any(), any(), any())).thenReturn(fakePdf);

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, validYear, 7, 2, null
            );

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }

        @Test
        @DisplayName("レスポンスボディにPDFバイナリが含まれる")
        void returnsBody_withPdfBytes() throws Exception {
            // 準備
            Member member = createSampleMember();
            CommonSettings settings = createSampleSettings();
            byte[] fakePdf = "%PDF-1.4 test content here".getBytes();

            when(memberService.findById(sampleMemberId)).thenReturn(member);
            when(commonSettingsService.find()).thenReturn(settings);
            when(pdfService.generate(any(), any(), any())).thenReturn(fakePdf);

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, validYear, 7, 2, null
            );

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(fakePdf));
        }

        @Test
        @DisplayName("申請年が当年なら200を返す")
        void returns200_withCurrentYear() throws Exception {
            // 準備
            int currentYear = LocalDate.now().getYear();
            when(memberService.findById(sampleMemberId)).thenReturn(createSampleMember());
            when(commonSettingsService.find()).thenReturn(createSampleSettings());
            when(pdfService.generate(any(), any(), any())).thenReturn("%PDF-1.4".getBytes());

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, currentYear, 7, 2, null);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("申請年が翌年なら200を返す")
        void returns200_withNextYear() throws Exception {
            // 準備
            int nextYear = LocalDate.now().getYear() + 1;
            when(memberService.findById(sampleMemberId)).thenReturn(createSampleMember());
            when(commonSettingsService.find()).thenReturn(createSampleSettings());
            when(pdfService.generate(any(), any(), any())).thenReturn("%PDF-1.4".getBytes());

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, nextYear, 7, 2, null);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("利用者名と申請年月日を含むファイル名を返す")
    void returnsFileNameWithMemberNameAndApplicationDate() throws Exception {

        Member member = createSampleMember();
        CommonSettings settings = createSampleSettings();

        when(memberService.findById(sampleMemberId)).thenReturn(member);
        when(commonSettingsService.find()).thenReturn(settings);
        when(pdfService.generate(any(), any(), any()))
                .thenReturn("%PDF-1.4".getBytes());

        PdfGenerateRequest request =
                new PdfGenerateRequest(
                        sampleMemberId,
                        validYear,
                        7,
                        2,
                        null
                );

        String expectedFileName =
                String.format(
                        "%04d年7月2日 田中太郎様 介護認定申請書.pdf",
                        validYear
                );

        String encodedFileName =
                URLEncoder.encode(
                        expectedFileName,
                        StandardCharsets.UTF_8
                ).replace("+", "%20");

        mockMvc.perform(post("/api/pdf/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName
                ));
    }

    // ========================================
    // 境界値
    // ========================================

    @Nested
    @DisplayName("境界値")
    class Boundary {

        @Test
        @DisplayName("変更理由が空文字でも200を返す")
        void returns200_withEmptyChangeReason() throws Exception {
            // 準備
            Member member = createSampleMember();
            CommonSettings settings = createSampleSettings();
            byte[] fakePdf = "%PDF-1.4 empty reason".getBytes();

            when(memberService.findById(sampleMemberId)).thenReturn(member);
            when(commonSettingsService.find()).thenReturn(settings);
            when(pdfService.generate(any(), any(), any())).thenReturn(fakePdf);

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, validYear, 7, 2, ""
            );

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }

        @Test
        @DisplayName("申請日が1月1日でも200を返す")
        void returns200_withJanuary1st() throws Exception {
            // 準備
            Member member = createSampleMember();
            CommonSettings settings = createSampleSettings();
            byte[] fakePdf = "%PDF-1.4 jan1".getBytes();

            when(memberService.findById(sampleMemberId)).thenReturn(member);
            when(commonSettingsService.find()).thenReturn(settings);
            when(pdfService.generate(any(), any(), any())).thenReturn(fakePdf);

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, validYear, 1, 1, null
            );

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("申請日が12月31日でも200を返す")
        void returns200_withDecember31st() throws Exception {
            // 準備
            Member member = createSampleMember();
            CommonSettings settings = createSampleSettings();
            byte[] fakePdf = "%PDF-1.4 dec31".getBytes();

            when(memberService.findById(sampleMemberId)).thenReturn(member);
            when(commonSettingsService.find()).thenReturn(settings);
            when(pdfService.generate(any(), any(), any())).thenReturn(fakePdf);

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, validYear, 12, 31, null
            );

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    // ========================================
    // 異常系
    // ========================================

    @Nested
    @DisplayName("異常系")
    class Error {

        // --- リソース不在（404）---

        @Test
        @DisplayName("存在しないmemberIdで404を返す")
        void returns404_whenMemberNotFound() throws Exception {
            // 準備
            UUID nonExistentId = UUID.randomUUID();
            when(memberService.findById(nonExistentId))
                    .thenThrow(new ResourceNotFoundException("利用者が見つかりません: " + nonExistentId));

            PdfGenerateRequest request = new PdfGenerateRequest(
                    nonExistentId, validYear, 7, 2, null
            );

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("共通設定が未登録で404を返す")
        void returns404_whenSettingsNotFound() throws Exception {
            // 準備
            Member member = createSampleMember();
            when(memberService.findById(sampleMemberId)).thenReturn(member);
            when(commonSettingsService.find())
                    .thenThrow(new ResourceNotFoundException("共通設定が登録されていません"));

            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, validYear, 7, 2, null
            );

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        // --- 必須項目の欠落（400）---

        @Test
        @DisplayName("memberIdなしで400を返す")
        void returns400_whenMemberIdMissing() throws Exception {
            // 準備
            String invalidJson = """
                    {
                        "applicationYear": 2026,
                        "applicationMonth": 7,
                        "applicationDay": 2
                    }
                    """;

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("applicationYearなしで400を返す")
        void returns400_whenApplicationYearMissing() throws Exception {
            // 準備
            String invalidJson = String.format("""
                    {
                        "memberId": "%s",
                        "applicationMonth": 7,
                        "applicationDay": 2
                    }
                    """, sampleMemberId);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("applicationMonthなしで400を返す")
        void returns400_whenApplicationMonthMissing() throws Exception {
            // 準備
            String invalidJson = String.format("""
                    {
                        "memberId": "%s",
                        "applicationYear": 2026,
                        "applicationDay": 2
                    }
                    """, sampleMemberId);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("applicationDayなしで400を返す")
        void returns400_whenApplicationDayMissing() throws Exception {
            // 準備
            String invalidJson = String.format("""
                    {
                        "memberId": "%s",
                        "applicationYear": 2026,
                        "applicationMonth": 7
                    }
                    """, sampleMemberId);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("リクエストボディが空の場合、エラーを返す")
        void returnsError_whenBodyIsEmpty() throws Exception {
            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        // --- 申請年月日の妥当性（400）---

        @Test
        @DisplayName("申請年が過去年（前年）の場合400を返す")
        void returns400_withPastYear() throws Exception {
            // 準備
            int pastYear = LocalDate.now().getYear() - 1;
            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, pastYear, 7, 2, null);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("申請年が翌々年（2年後）の場合400を返す")
        void returns400_withTooFutureYear() throws Exception {
            // 準備
            int tooFutureYear = LocalDate.now().getYear() + 2;
            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, tooFutureYear, 7, 2, null);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("申請月が13の場合400を返す")
        void returns400_withInvalidMonth() throws Exception {
            // 準備
            int currentYear = LocalDate.now().getYear();
            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, currentYear, 13, 2, null);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("2月30日など存在しない日付の場合400を返す")
        void returns400_withNonExistentDate() throws Exception {
            // 準備: 2月30日は存在しない
            int currentYear = LocalDate.now().getYear();
            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, currentYear, 2, 30, null);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("4月31日（30日までの月に31日）の場合400を返す")
        void returns400_withApril31st() throws Exception {
            // 準備: 4月は30日まで
            int currentYear = LocalDate.now().getYear();
            PdfGenerateRequest request = new PdfGenerateRequest(
                    sampleMemberId, currentYear, 4, 31, null);

            // 実行 & 検証
            mockMvc.perform(post("/api/pdf/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
