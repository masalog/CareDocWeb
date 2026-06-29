package com.example.CareDocWeb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 利用者エンティティ。
 *
 * <p>介護保険の要介護認定・要支援認定 申請書に記載される利用者情報を保持する。
 * DBの {@code members} テーブルに対応し、1レコードが1人の利用者を表す。</p>
 *
 * <p>主キーはUUID自動生成（Supabase互換）。
 * 認定終了日・施設入所日は未設定の場合があるため NULL 許容。</p>
 *
 * @see com.example.CareDocWeb.repository.MemberRepository
 */
@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    /** 主キー（UUID自動生成） */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 被保険者番号（介護保険証に記載の10桁番号） */
    @Column(name = "insurance_id_number")
    private String insuranceIdNumber;

    /** 氏名 */
    @Column(nullable = false)
    private String name;

    /** フリガナ（カタカナ） */
    private String furigana;

    /** 生年月日（年）— 西暦4桁 */
    @Column(name = "birth_year")
    private Integer birthYear;

    /** 生年月日（月） */
    @Column(name = "birth_month")
    private Integer birthMonth;

    /** 生年月日（日） */
    @Column(name = "birth_day")
    private Integer birthDay;

    /** 性別（"男" / "女"） */
    private String gender;

    /** 住所 */
    private String address;

    /** 電話番号 */
    private String phone;

    /** 介護度（例: "要介護1", "要支援2"） */
    @Column(name = "care_level")
    private String careLevel;

    /** 認定有効期間 開始日（年）— 西暦4桁 */
    @Column(name = "start_year")
    private Integer startYear;

    /** 認定有効期間 開始日（月） */
    @Column(name = "start_month")
    private Integer startMonth;

    /** 認定有効期間 開始日（日） */
    @Column(name = "start_day")
    private Integer startDay;

    /** 認定有効期間 終了日（年）— 未設定時はNULL */
    @Column(name = "end_year")
    private Integer endYear;

    /** 認定有効期間 終了日（月）— 未設定時はNULL */
    @Column(name = "end_month")
    private Integer endMonth;

    /** 認定有効期間 終了日（日）— 未設定時はNULL */
    @Column(name = "end_day")
    private Integer endDay;

    /** 施設入所日（年）— 入所していない場合はNULL */
    @Column(name = "institution_year")
    private Integer institutionYear;

    /** 施設入所日（月）— 入所していない場合はNULL */
    @Column(name = "institution_month")
    private Integer institutionMonth;

    /** 施設入所日（日）— 入所していない場合はNULL */
    @Column(name = "institution_day")
    private Integer institutionDay;

    /** レコード作成日時（自動設定・更新不可） */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** レコード更新日時（更新時に自動設定） */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
