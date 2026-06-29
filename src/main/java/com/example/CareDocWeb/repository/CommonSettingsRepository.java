package com.example.CareDocWeb.repository;

import com.example.CareDocWeb.entity.CommonSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommonSettingsRepository extends JpaRepository<CommonSettings, UUID> {
}
