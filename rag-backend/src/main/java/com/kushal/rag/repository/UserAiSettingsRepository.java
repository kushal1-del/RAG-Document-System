package com.kushal.rag.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kushal.rag.entity.UserAiSettings;

public interface UserAiSettingsRepository extends JpaRepository<UserAiSettings, String> {

    Optional<UserAiSettings> findByUserId(String userId);
}