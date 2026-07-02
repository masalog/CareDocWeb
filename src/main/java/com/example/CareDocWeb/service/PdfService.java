package com.example.CareDocWeb.service;

import com.example.CareDocWeb.dto.PdfGenerateRequest;
import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.entity.Member;

/**
 * 申請書PDF生成サービス。
 *
 * <p>利用者データと共通設定をテンプレートPDFに転記し、
 * 完成したPDFのバイト配列を返す。</p>
 */
public interface PdfService {

    /**
     * 申請書PDFを生成する。
     *
     * @param member   利用者データ
     * @param settings 共通設定データ
     * @param request  PDF生成リクエスト（申請日・変更理由）
     * @return PDF のバイト配列
     */
    byte[] generate(Member member, CommonSettings settings, PdfGenerateRequest request);
}
