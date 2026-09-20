package com.kushal.rag.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kushal.rag.entity.User;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}