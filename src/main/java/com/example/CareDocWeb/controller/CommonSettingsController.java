package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.service.CommonSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 共通設定コントローラー。
 *
 * <p>事業所全体の共通設定に関するAPIエンドポイントを提供する。
 * API設計書に基づき、共通データの取得・更新を実装する。</p>
 *
 * <ul>
 *   <li>{@code GET /api/settings} — 共通データを取得</li>
 *   <li>{@code PUT /api/settings} — 共通データを更新</li>
 * </ul>
 *
 * @see CommonSettingsService
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class CommonSettingsController {

    private final CommonSettingsService commonSettingsService;

    /**
     * 共通設定を取得する。
     *
     * @return 共通設定データ
     */
    @GetMapping
    public ResponseEntity<CommonSettings> find() {
        CommonSettings settings = commonSettingsService.find();
        return ResponseEntity.ok(settings);
    }

    /**
     * 共通設定を更新する。
     *
     * @param settings 更新する共通設定データ
     * @return 更新後の共通設定
     */
    @PutMapping
    public ResponseEntity<CommonSettings> update(@RequestBody CommonSettings settings) {
        CommonSettings updatedSettings = commonSettingsService.save(settings);
        return ResponseEntity.ok(updatedSettings);
    }
}
