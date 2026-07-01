package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.exception.ResourceNotFoundException;
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
 * API設計書に基づき、利用者のCRUD操作を実装する。</p>
 *
 * <ul>
 *   <li>{@code GET /api/members} — 利用者一覧を取得</li>
 *   <li>{@code GET /api/members/{id}} — 利用者詳細を取得</li>
 *   <li>{@code POST /api/members} — 利用者を登録</li>
 *   <li>{@code PUT /api/members/{id}} — 利用者を更新</li>
 *   <li>{@code DELETE /api/members/{id}} — 利用者を削除</li>
 * </ul>
 *
 * @see MemberService
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 利用者一覧を取得する。
     *
     * @return 利用者のリスト
     */
    @GetMapping
    public ResponseEntity<List<Member>> findAll() {
        List<Member> members = memberService.findAll();
        return ResponseEntity.ok(members);
    }

    /**
     * IDを指定して利用者詳細を取得する。
     *
     * @param id 利用者のUUID
     * @return 該当する利用者
     */
    @GetMapping("/{id}")
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
    @PostMapping
    public ResponseEntity<Member> create(@RequestBody Member member) {
        Member savedMember = memberService.save(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMember);
    }

    /**
     * IDを指定して利用者を更新する。
     *
     * <p>指定IDの利用者が存在しない場合は404を返す。</p>
     *
     * @param id 更新対象の利用者UUID
     * @param member 更新データ
     * @return 更新後の利用者
     */
    @PutMapping("/{id}")
    public ResponseEntity<Member> update(@PathVariable UUID id, @RequestBody Member member) {
        if (!memberService.existsById(id)) {
            throw new ResourceNotFoundException("利用者が見つかりません: " + id);
        }
        member.setId(id);
        Member updatedMember = memberService.save(member);
        return ResponseEntity.ok(updatedMember);
    }

    /**
     * IDを指定して利用者を削除する。
     *
     * <p>指定IDの利用者が存在しない場合は404を返す。</p>
     *
     * @param id 削除対象の利用者UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!memberService.existsById(id)) {
            throw new ResourceNotFoundException("利用者が見つかりません: " + id);
        }
        memberService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
