package com.kushal.rag.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_api_key")
public class UserApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // User who owns this API key.
    // We store the user ID instead of creating a JPA relationship
    // so the API-key system remains independent from the User entity.
    @Column(nullable = false)
    private String userId;

    // Provider of this API key.
    // Currently Gemini, but this allows additional providers later.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiKeyProvider provider;

    // User-friendly name such as "Personal Gemini Key".
    @Column(nullable = false)
    private String label;

    // Encrypted API key.
    // NEVER store the actual API key as plaintext in the database.
    @Column(nullable = false, length = 2000)
    private String encryptedKey;

    // User can select one configured key for answer generation.
    @Column(nullable = false)
    private boolean primaryKey = false;

    // Disabled keys cannot be used for answer generation.
    @Column(nullable = false)
    private boolean enabled = true;

    // API key automatically becomes unusable after this date/time.
    private LocalDateTime expiresAt;

    // Useful for tracking when the key was created.
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Useful for tracking when the key was last modified.
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public UserApiKey() {
    }

    public UserApiKey(
            String userId,
            ApiKeyProvider provider,
            String label,
            String encryptedKey,
            LocalDateTime expiresAt) {

        this.userId = userId;
        this.provider = provider;
        this.label = label;
        this.encryptedKey = encryptedKey;
        this.expiresAt = expiresAt;
        this.primaryKey = false;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    public ApiKeyProvider getProvider() {
        return provider;
    }

    public void setProvider(ApiKeyProvider provider) {
        this.provider = provider;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        this.updatedAt = LocalDateTime.now();
    }

    public String getEncryptedKey() {
        return encryptedKey;
    }

    public void setEncryptedKey(String encryptedKey) {
        this.encryptedKey = encryptedKey;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}