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
 * 共通設定エンティティ。
 *
 * <p>事業所全体で共有する設定値（調査先、施設、代理人、クリニック情報等）を保持する。
 * DBの {@code common_settings} テーブルに対応し、テーブル内には常に1レコードのみ存在する。</p>
 *
 * <p>申請書PDFに転記される担当者・医療機関の情報をまとめて管理する。
 * 利用者（{@link Member}）とはリレーションを持たず独立している。</p>
 *
 * @see com.example.CareDocWeb.repository.CommonSettingsRepository
 */
@Entity
@Table(name = "common_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonSettings {

    /** 主キー（UUID自動生成） */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 調査連絡先 — 住所 */
    @Column(name = "survey_address")
    private String surveyAddress;

    /** 調査連絡先 — 電話番号 */
    @Column(name = "survey_phone")
    private String surveyPhone;

    /** 入所施設名 */
    @Column(name = "facility_name")
    private String facilityName;

    /** 入所施設 — 電話番号 */
    @Column(name = "facility_phone")
    private String facilityPhone;

    /** 医療機関名（主治医意見書の提出先） */
    @Column(name = "institution_name")
    private String institutionName;

    /** 医療機関 — 住所 */
    @Column(name = "institution_address")
    private String institutionAddress;

    /** 申請代理人 — 氏名 */
    @Column(name = "agent_name")
    private String agentName;

    /** 申請代理人 — 郵便番号 */
    @Column(name = "agent_postal")
    private String agentPostal;

    /** 申請代理人 — 住所 */
    @Column(name = "agent_address")
    private String agentAddress;

    /** 申請代理人 — 電話番号 */
    @Column(name = "agent_phone")
    private String agentPhone;

    /** 主治医 — 氏名 */
    @Column(name = "doctor_name")
    private String doctorName;

    /** 主治医のクリニック — 名称 */
    @Column(name = "clinic_name")
    private String clinicName;

    /** 主治医のクリニック — 郵便番号 */
    @Column(name = "clinic_postal")
    private String clinicPostal;

    /** 主治医のクリニック — 住所 */
    @Column(name = "clinic_address")
    private String clinicAddress;

    /** 主治医のクリニック — 電話番号 */
    @Column(name = "clinic_phone")
    private String clinicPhone;

    /** レコード作成日時（自動設定・更新不可） */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** レコード更新日時（更新時に自動設定） */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
