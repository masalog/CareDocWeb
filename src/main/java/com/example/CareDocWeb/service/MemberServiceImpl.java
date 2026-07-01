package com.example.CareDocWeb.service;

import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.exception.ResourceNotFoundException;
import com.example.CareDocWeb.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 利用者サービス実装クラス。
 *
 * <p>{@link MemberService} の実装。
 * {@link MemberRepository} を通じて利用者データのCRUD操作を提供する。</p>
 *
 * <p>更新・削除系のメソッドには {@code @Transactional} を付与し、
 * 存在確認と操作を同一トランザクション内で完結させることで競合状態を防止する。</p>
 *
 * @see MemberService
 * @see MemberRepository
 */
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Member findById(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("利用者が見つかりません: " + id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Member save(Member member) {
        return memberRepository.save(member);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Member update(UUID id, Member member) {
        // 存在確認と更新を同一トランザクション内で実行
        findById(id);
        member.setId(id);
        return memberRepository.save(member);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(UUID id) {
        // 存在確認と削除を同一トランザクション内で実行
        findById(id);
        memberRepository.deleteById(id);
    }
}
