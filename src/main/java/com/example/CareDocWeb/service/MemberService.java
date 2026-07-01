package com.example.CareDocWeb.service;

import com.example.CareDocWeb.entity.Member;

import java.util.List;
import java.util.UUID;

/**
 * 利用者サービスインタフェース。
 *
 * <p>利用者（{@link Member}）に関するビジネスロジックの契約を定義する。
 * コントローラー層はこのインタフェースに依存し、実装の詳細を知らずに利用できる。</p>
 *
 * @see MemberServiceImpl
 */
public interface MemberService {

    /**
     * 全利用者を取得する。
     *
     * @return 利用者のリスト（0件の場合は空リスト）
     */
    List<Member> findAll();

    /**
     * IDを指定して利用者を1件取得する。
     *
     * @param id 利用者のUUID
     * @return 該当する利用者
     * @throws com.example.CareDocWeb.exception.ResourceNotFoundException 指定IDの利用者が存在しない場合
     */
    Member findById(UUID id);

    /**
     * 利用者を新規登録する。
     *
     * @param member 登録する利用者エンティティ
     * @return 保存後の利用者エンティティ（ID・タイムスタンプが設定済み）
     */
    Member save(Member member);

    /**
     * IDを指定して利用者を更新する。
     *
     * <p>存在確認と更新を同一トランザクション内で行い、競合状態を防止する。
     * 指定IDの利用者が存在しない場合は例外をスローする。</p>
     *
     * @param id 更新対象の利用者UUID
     * @param member 更新データ
     * @return 更新後の利用者エンティティ
     * @throws com.example.CareDocWeb.exception.ResourceNotFoundException 指定IDの利用者が存在しない場合
     */
    Member update(UUID id, Member member);

    /**
     * IDを指定して利用者を削除する。
     *
     * <p>存在確認と削除を同一トランザクション内で行い、競合状態を防止する。
     * 指定IDの利用者が存在しない場合は例外をスローする。</p>
     *
     * @param id 削除対象の利用者UUID
     * @throws com.example.CareDocWeb.exception.ResourceNotFoundException 指定IDの利用者が存在しない場合
     */
    void delete(UUID id);
}
