package com.example.CareDocWeb.repository;

import com.example.CareDocWeb.entity.CommonSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * 共通設定リポジトリ。
 *
 * <p>{@link CommonSettings} エンティティに対するCRUD操作を提供する。
 * Spring Data JPA により、インタフェース定義のみで基本的なデータアクセス処理が自動実装される。</p>
 *
 * <p>{@code common_settings} テーブルには事業所全体で1レコードのみ存在する想定。
 * 取得時は {@code findAll()} で取得し、先頭の1件を使用する。</p>
 *
 * @see CommonSettings
 */
@Repository
public interface CommonSettingsRepository extends JpaRepository<CommonSettings, UUID> {
}
