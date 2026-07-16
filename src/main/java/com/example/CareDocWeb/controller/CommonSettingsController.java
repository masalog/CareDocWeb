package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.service.CommonSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 共通設定コントローラー（管理API）。
 *
 * <p>事業所全体の共通設定に関するAPIエンドポイントを提供する。
 * 管理画面（admin.html）専用であり、/api/admin/** 配下は API Gateway の
 * Cognito Authorizer により認証必須で、有効な ID トークンなしでは
 * リクエストがこのアプリに到達しない。</p>
 *
 * <ul>
 *   <li>{@code GET /api/admin/settings} — 共通データを取得</li>
 *   <li>{@code PUT /api/admin/settings} — 共通データを更新</li>
 * </ul>
 *
 * @see CommonSettingsService
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class CommonSettingsController {

    private final CommonSettingsService commonSettingsService;

    @Value("${READ_ONLY_MODE:true}")
    private boolean readOnly;

    /**
     * 共通設定を取得する。
     */
    @GetMapping
    public ResponseEntity<CommonSettings> find() {
        CommonSettings settings = commonSettingsService.find();
        return ResponseEntity.ok(settings);
    }

    /**
     * 共通設定を更新する（閲覧専用モードでは禁止）。
     */
    @PutMapping
    public ResponseEntity<?> update(@RequestBody CommonSettings settings) {
        if (readOnly) {
            return ResponseEntity.status(403)
                    .body("現在は閲覧専用モードのため、共通設定の更新はできません。");
        }

        CommonSettings updatedSettings = commonSettingsService.save(settings);
        return ResponseEntity.ok(updatedSettings);
    }
}
