package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 利用者コントローラー。
 *
 * <p>利用者に関するAPIエンドポイントを提供する。
 * 公開APIと管理APIの2系統を持つ。</p>
 *
 * <p>【公開API】認証不要。index.html（PDF生成ページ）が使用する。</p>
 * <ul>
 *   <li>{@code GET /api/members} — 利用者一覧を取得</li>
 * </ul>
 *
 * <p>【管理API】admin.html 専用。/api/admin/** 配下は API Gateway の
 * Cognito Authorizer により認証必須で、有効な ID トークンなしでは
 * リクエストがこのアプリに到達しない。</p>
 * <ul>
 *   <li>{@code GET /api/admin/members/{id}} — 利用者詳細を取得</li>
 *   <li>{@code POST /api/admin/members} — 利用者を登録</li>
 *   <li>{@code PUT /api/admin/members/{id}} — 利用者を更新</li>
 *   <li>{@code DELETE /api/admin/members/{id}} — 利用者を削除</li>
 * </ul>
 *
 * @see MemberService
 */
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // ========================================
    // 公開API（認証不要）
    // ========================================

    /**
     * 利用者一覧を取得する。
     *
     * @return 利用者のリスト
     */
    @GetMapping("/api/members")
    public ResponseEntity<List<Member>> findAll() {
        List<Member> members = memberService.findAll();
        return ResponseEntity.ok(members);
    }

    // ========================================
    // 管理API（Cognito 認証必須）
    // ========================================

    /**
     * IDを指定して利用者詳細を取得する。
     *
     * @param id 利用者のUUID
     * @return 該当する利用者
     */
    @GetMapping("/api/admin/members/{id}")
    public ResponseEntity<Member> findById(@PathVariable UUID id) {
        Member member = memberService.findById(id);
        return ResponseEntity.ok(member);
    }

    /**
     * 利用者を新規登録する。
     *
     * @param member 登録する利用者データ
     * @return 登録後の利用者（ID・タイムスタンプ付き）
     */
    @PostMapping("/api/admin/members")
    public ResponseEntity<Member> create(@RequestBody Member member) {
        Member savedMember = memberService.save(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMember);
    }

    /**
     * IDを指定して利用者を更新する。
     *
     * <p>指定IDの利用者が存在しない場合は404を返す。
     * 存在確認と更新はサービス層で同一トランザクション内に処理される。</p>
     *
     * @param id 更新対象の利用者UUID
     * @param member 更新データ
     * @return 更新後の利用者
     */
    @PutMapping("/api/admin/members/{id}")
    public ResponseEntity<Member> update(@PathVariable UUID id, @RequestBody Member member) {
        Member updatedMember = memberService.update(id, member);
        return ResponseEntity.ok(updatedMember);
    }

    /**
     * IDを指定して利用者を削除する。
     *
     * <p>指定IDの利用者が存在しない場合は404を返す。
     * 存在確認と削除はサービス層で同一トランザクション内に処理される。</p>
     *
     * @param id 削除対象の利用者UUID
     * @return 204 No Content
     */
    @DeleteMapping("/api/admin/members/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}