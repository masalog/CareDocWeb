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
@Table(name = "common_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 調査先住所 */
    @Column(name = "survey_address")
    private String surveyAddress;

    /** 調査先電話 */
    @Column(name = "survey_phone")
    private String surveyPhone;

    /** 施設名 */
    @Column(name = "facility_name")
    private String facilityName;

    /** 施設電話 */
    @Column(name = "facility_phone")
    private String facilityPhone;

    /** 医療機関名 */
    @Column(name = "institution_name")
    private String institutionName;

    /** 医療機関住所 */
    @Column(name = "institution_address")
    private String institutionAddress;

    /** 代理人氏名 */
    @Column(name = "agent_name")
    private String agentName;

    /** 代理人郵便番号 */
    @Column(name = "agent_postal")
    private String agentPostal;

    /** 代理人住所 */
    @Column(name = "agent_address")
    private String agentAddress;

    /** 代理人電話 */
    @Column(name = "agent_phone")
    private String agentPhone;

    /** 主治医氏名 */
    @Column(name = "doctor_name")
    private String doctorName;

    /** クリニック名 */
    @Column(name = "clinic_name")
    private String clinicName;

    /** クリニック郵便番号 */
    @Column(name = "clinic_postal")
    private String clinicPostal;

    /** クリニック住所 */
    @Column(name = "clinic_address")
    private String clinicAddress;

    /** クリニック電話 */
    @Column(name = "clinic_phone")
    private String clinicPhone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
