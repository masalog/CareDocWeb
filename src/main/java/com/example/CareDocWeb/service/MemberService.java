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
     * @throws RuntimeException 指定IDの利用者が存在しない場合
     */
    Member findById(UUID id);

    /**
     * 指定したIDの利用者が存在するか確認する。
     *
     * @param id 利用者のUUID
     * @return 存在する場合はtrue、存在しない場合はfalse
     */
    boolean existsById(UUID id);

    /**
     * 利用者を登録または更新する。
     *
     * <p>IDが未設定の場合は新規登録、設定済みの場合は更新として扱われる。</p>
     *
     * @param member 登録・更新する利用者エンティティ
     * @return 保存後の利用者エンティティ（ID・タイムスタンプが設定済み）
     */
    Member save(Member member);

    /**
     * IDを指定して利用者を削除する。
     *
     * @param id 削除対象の利用者UUID
     */
    void deleteById(UUID id);
}
