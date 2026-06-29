package com.example.CareDocWeb.service;

import com.example.CareDocWeb.entity.CommonSettings;

/**
 * 共通設定サービスインタフェース。
 *
 * <p>事業所全体で共有する設定値（{@link CommonSettings}）に関するビジネスロジックの契約を定義する。
 * {@code common_settings} テーブルには常に1レコードのみ存在する前提で設計されている。</p>
 *
 * @see CommonSettingsServiceImpl
 */
public interface CommonSettingsService {

    /**
     * 共通設定を取得する。
     *
     * <p>テーブル内の唯一のレコードを返す。
     * レコードが存在しない場合は例外をスローする。</p>
     *
     * @return 共通設定エンティティ
     * @throws RuntimeException 共通設定が未登録の場合
     */
    CommonSettings find();

    /**
     * 共通設定を更新する。
     *
     * <p>既存レコードのIDを保持したまま、各フィールドの値を上書き保存する。</p>
     *
     * @param settings 更新する共通設定エンティティ
     * @return 保存後の共通設定エンティティ
     */
    CommonSettings save(CommonSettings settings);
}
