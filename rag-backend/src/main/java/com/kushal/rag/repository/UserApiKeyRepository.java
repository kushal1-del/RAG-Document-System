package com.kushal.rag.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kushal.rag.entity.UserApiKey;

public interface UserApiKeyRepository extends JpaRepository<UserApiKey, String> {

    List<UserApiKey> findByUserId(String userId);

    Optional<UserApiKey> findByIdAndUserId(String id, String userId);
}