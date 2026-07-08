package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.dto.PdfGenerateRequest;
import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.service.CommonSettingsService;
import com.example.CareDocWeb.service.MemberService;
import com.example.CareDocWeb.service.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * PDF生成コントローラー。
 *
 * <p>利用者データと共通設定を束ね、PDFを生成してバイナリレスポンスとして返す。</p>
 */
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final MemberService memberService;
    private final CommonSettingsService commonSettingsService;
    private final PdfService pdfService;

    /**
     * 申請書PDFを生成してダウンロードする。
     *
     * @param request 利用者ID・申請日・変更理由を含むリクエスト
     * @return PDF バイナリ（application/pdf）
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody PdfGenerateRequest request) {

        Member member = memberService.findById(request.getMemberId());
        CommonSettings settings = commonSettingsService.find();
        byte[] pdf = pdfService.generate(member, settings, request);

        String applicationDate = String.format(
                "%04d年%d月%d日",
                request.getApplicationYear(),
                request.getApplicationMonth(),
                request.getApplicationDay()
        );

        String fileName =
                applicationDate
                        + " "
                        + member.getName()
                        + "様 介護認定申請書.pdf";

        String encodedFileName = URLEncoder.encode(
                fileName,
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}