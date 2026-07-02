package com.example.CareDocWeb.service;

import com.example.CareDocWeb.dto.PdfGenerateRequest;
import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.service.LayoutLoader.FieldPosition;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * PDFBox を使ったPDF生成サービスの実装。
 *
 * <p>テンプレートPDFに座標YAMLで定義された位置にテキストを描画し、
 * 完成したPDFをバイト配列として返す。</p>
 */
@Service
public class PdfServiceImpl implements PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfServiceImpl.class);

    @Override
    public byte[] generate(Member member, CommonSettings settings, PdfGenerateRequest request) {
        Map<String, FieldPosition> layout = LayoutLoader.loadLayout();

        try (InputStream templateStream = getTemplate();
             PDDocument doc = Loader.loadPDF(templateStream.readAllBytes())) {

            PDPage page = doc.getPage(0);
            PDFont font = PDType0Font.load(doc, getFont());

            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.APPEND, true)) {

                // --- 利用者データ ---
                drawText(cs, font, layout, "Insurance ID Number", nullSafe(member.getInsuranceIdNumber()));
                drawText(cs, font, layout, "name", nullSafe(member.getName()));
                drawText(cs, font, layout, "furigana", nullSafe(member.getFurigana()));

                drawTextFromInt(cs, font, layout, "birthYear", member.getBirthYear());
                drawTextFromInt(cs, font, layout, "birthMonth", member.getBirthMonth());
                drawTextFromInt(cs, font, layout, "birthDay", member.getBirthDay());

                drawText(cs, font, layout, "address", nullSafe(member.getAddress()));
                drawText(cs, font, layout, "phone", nullSafe(member.getPhone()));

                // 性別（〇で囲む）
                if ("男".equals(member.getGender())) {
                    drawCircle(cs, font, layout, "genderMale");
                } else if ("女".equals(member.getGender())) {
                    drawCircle(cs, font, layout, "genderFemale");
                }

                // 介護度（〇で囲む）
                String careKey = mapCareLevel(member.getCareLevel());
                if (careKey != null) {
                    drawCircle(cs, font, layout, careKey);
                }

                // 有効期間
                drawTextFromInt(cs, font, layout, "startYear", member.getStartYear());
                drawTextFromInt(cs, font, layout, "startMonth", member.getStartMonth());
                drawTextFromInt(cs, font, layout, "startDay", member.getStartDay());
                drawTextFromInt(cs, font, layout, "endYear", member.getEndYear());
                drawTextFromInt(cs, font, layout, "endMonth", member.getEndMonth());
                drawTextFromInt(cs, font, layout, "endDay", member.getEndDay());

                // 入所日
                drawTextFromInt(cs, font, layout, "institutionYear", member.getInstitutionYear());
                drawTextFromInt(cs, font, layout, "institutionMonth", member.getInstitutionMonth());
                drawTextFromInt(cs, font, layout, "institutionDay", member.getInstitutionDay());

                // --- 共通設定 ---
                drawText(cs, font, layout, "Survey Location Address", nullSafe(settings.getSurveyAddress()));
                drawText(cs, font, layout, "Survey Location Phone", nullSafe(settings.getSurveyPhone()));
                drawText(cs, font, layout, "facilityName", nullSafe(settings.getFacilityName()));
                drawText(cs, font, layout, "facilityPhone", nullSafe(settings.getFacilityPhone()));
                drawText(cs, font, layout, "institutionName", nullSafe(settings.getInstitutionName()));
                drawText(cs, font, layout, "institutionAddress", nullSafe(settings.getInstitutionAddress()));
                drawText(cs, font, layout, "agentName", nullSafe(settings.getAgentName()));
                drawText(cs, font, layout, "agentPostal", nullSafe(settings.getAgentPostal()));
                drawText(cs, font, layout, "agentAddress", nullSafe(settings.getAgentAddress()));
                drawText(cs, font, layout, "agentPhone", nullSafe(settings.getAgentPhone()));
                drawText(cs, font, layout, "doctorName", nullSafe(settings.getDoctorName()));
                drawText(cs, font, layout, "clinicName", nullSafe(settings.getClinicName()));
                drawText(cs, font, layout, "clinicPostal", nullSafe(settings.getClinicPostal()));
                drawText(cs, font, layout, "clinicAddress", nullSafe(settings.getClinicAddress()));
                drawText(cs, font, layout, "clinicPhone", nullSafe(settings.getClinicPhone()));

                drawCircle(cs, font, layout, "isFacility");
                drawCircle(cs, font, layout, "agentCategory");

                // --- 申請日 ---
                drawTextFromInt(cs, font, layout, "applyYear", request.getApplicationYear());
                drawTextFromInt(cs, font, layout, "applyMonth", request.getApplicationMonth());
                drawTextFromInt(cs, font, layout, "applyDay", request.getApplicationDay());

                // --- 変更理由 ---
                drawText(cs, font, layout, "Change Request Reason", nullSafe(request.getChangeReason()));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("PDF生成中にエラーが発生しました", e);
            throw new RuntimeException("PDF生成に失敗しました", e);
        }
    }

    // ===================== private helpers =====================

    private void drawText(PDPageContentStream cs, PDFont font,
                          Map<String, FieldPosition> layout, String key, String value) throws IOException {
        if (value == null || value.isEmpty()) return;
        FieldPosition pos = layout.get(key);
        if (pos == null) return;

        String[] lines = value.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            cs.beginText();
            cs.setFont(font, pos.fontSize());
            cs.newLineAtOffset(pos.x(), pos.y() - i * (pos.fontSize() + 2));
            cs.showText(lines[i]);
            cs.endText();
        }
    }

    private void drawTextFromInt(PDPageContentStream cs, PDFont font,
                                 Map<String, FieldPosition> layout, String key, Integer value) throws IOException {
        if (value == null) return;
        drawText(cs, font, layout, key, value.toString());
    }

    private void drawCircle(PDPageContentStream cs, PDFont font,
                            Map<String, FieldPosition> layout, String key) throws IOException {
        FieldPosition pos = layout.get(key);
        if (pos == null) return;
        cs.beginText();
        cs.setFont(font, pos.fontSize());
        cs.newLineAtOffset(pos.x(), pos.y());
        cs.showText("〇");
        cs.endText();
    }

    private String mapCareLevel(String careLevel) {
        if (careLevel == null) return null;
        return switch (careLevel) {
            case "要介護1" -> "Long-term Care Level 1";
            case "要介護2" -> "Long-term Care Level 2";
            case "要介護3" -> "Long-term Care Level 3";
            case "要介護4" -> "Long-term Care Level 4";
            case "要介護5" -> "Long-term Care Level 5";
            case "要支援1" -> "Support Level 1";
            case "要支援2" -> "Support Level 2";
            default -> null;
        };
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private InputStream getTemplate() {
        InputStream is = getClass().getResourceAsStream("/templates/template.pdf");
        if (is == null) {
            throw new IllegalStateException("テンプレートPDFが見つかりません: /templates/template.pdf");
        }
        return is;
    }

    private InputStream getFont() {
        InputStream is = getClass().getResourceAsStream("/fonts/NotoSansJP-Regular.ttf");
        if (is == null) {
            throw new IllegalStateException("フォントファイルが見つかりません: /fonts/NotoSansJP-Regular.ttf");
        }
        return is;
    }
}
