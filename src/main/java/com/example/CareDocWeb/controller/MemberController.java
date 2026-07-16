package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${READ_ONLY_MODE:true}")
    private boolean readOnly;

    // ========================================
    // 公開API（認証不要）
    // ========================================

    @GetMapping("/api/members")
    public ResponseEntity<List<Member>> findAll() {
        List<Member> members = memberService.findAll();
        return ResponseEntity.ok(members);
    }

    // ========================================
    // 管理API（Cognito 認証必須）
    // ========================================

    @GetMapping("/api/admin/members/{id}")
    public ResponseEntity<Member> findById(@PathVariable UUID id) {
        Member member = memberService.findById(id);
        return ResponseEntity.ok(member);
    }

    // ---- 編集禁止（閲覧専用モード） ----
    @PostMapping("/api/admin/members")
    public ResponseEntity<?> create(@RequestBody Member member) {
        if (readOnly) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("現在は閲覧専用モードのため、登録はできません。");
        }
        Member savedMember = memberService.save(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMember);
    }

    @PutMapping("/api/admin/members/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Member member) {
        if (readOnly) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("現在は閲覧専用モードのため、更新はできません。");
        }
        Member updatedMember = memberService.update(id, member);
        return ResponseEntity.ok(updatedMember);
    }

    @DeleteMapping("/api/admin/members/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        if (readOnly) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("現在は閲覧専用モードのため、削除はできません。");
        }
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
