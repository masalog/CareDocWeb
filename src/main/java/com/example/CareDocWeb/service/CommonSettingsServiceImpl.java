package com.example.CareDocWeb.service;

import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.exception.ResourceNotFoundException;
import com.example.CareDocWeb.repository.CommonSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 共通設定サービス実装クラス。
 *
 * <p>{@link CommonSettingsService} の実装。
 * {@link CommonSettingsRepository} を通じて共通設定の取得・更新を提供する。</p>
 *
 * <p>{@code common_settings} テーブルには1レコードのみ存在する前提。
 * 取得時は {@code findAll()} の先頭レコードを返す。</p>
 *
 * @see CommonSettingsService
 * @see CommonSettingsRepository
 */
@Service
@RequiredArgsConstructor
public class CommonSettingsServiceImpl implements CommonSettingsService {

    private final CommonSettingsRepository commonSettingsRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonSettings find() {
        return commonSettingsRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("共通設定が登録されていません"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CommonSettings save(CommonSettings settings) {
        // 既存レコードが存在する場合、そのIDを引き継いで上書きする（単一行の保証）
        commonSettingsRepository.findAll()
                .stream()
                .findFirst()
                .ifPresent(existing -> settings.setId(existing.getId()));
        return commonSettingsRepository.save(settings);
    }
}
