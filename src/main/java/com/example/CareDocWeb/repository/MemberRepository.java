package com.example.CareDocWeb.repository;

import com.example.CareDocWeb.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * 利用者リポジトリ。
 *
 * <p>{@link Member} エンティティに対するCRUD操作を提供する。
 * Spring Data JPA により、インタフェース定義のみで基本的なデータアクセス処理が自動実装される。</p>
 *
 * <p>自動で使用可能なメソッド例:</p>
 * <ul>
 *   <li>{@code findAll()} — 全利用者を取得</li>
 *   <li>{@code findById(UUID id)} — IDで1件取得</li>
 *   <li>{@code save(Member member)} — 登録または更新</li>
 *   <li>{@code deleteById(UUID id)} — IDで削除</li>
 * </ul>
 *
 * @see Member
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
}
