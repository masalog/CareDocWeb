package com.example.CareDocWeb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 被保険者番号 */
    @Column(name = "insurance_id_number")
    private String insuranceIdNumber;

    /** 氏名 */
    @Column(nullable = false)
    private String name;

    /** フリガナ */
    private String furigana;

    /** 生年月日（年） */
    @Column(name = "birth_year")
    private Integer birthYear;

    /** 生年月日（月） */
    @Column(name = "birth_month")
    private Integer birthMonth;

    /** 生年月日（日） */
    @Column(name = "birth_day")
    private Integer birthDay;

    /** 性別 */
    private String gender;

    /** 住所 */
    private String address;

    /** 電話番号 */
    private String phone;

    /** 介護度 */
    @Column(name = "care_level")
    private String careLevel;

    /** 認定開始日（年） */
    @Column(name = "start_year")
    private Integer startYear;

    /** 認定開始日（月） */
    @Column(name = "start_month")
    private Integer startMonth;

    /** 認定開始日（日） */
    @Column(name = "start_day")
    private Integer startDay;

    /** 認定終了日（年） */
    @Column(name = "end_year")
    private Integer endYear;

    /** 認定終了日（月） */
    @Column(name = "end_month")
    private Integer endMonth;

    /** 認定終了日（日） */
    @Column(name = "end_day")
    private Integer endDay;

    /** 施設入所日（年） */
    @Column(name = "institution_year")
    private Integer institutionYear;

    /** 施設入所日（月） */
    @Column(name = "institution_month")
    private Integer institutionMonth;

    /** 施設入所日（日） */
    @Column(name = "institution_day")
    private Integer institutionDay;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
