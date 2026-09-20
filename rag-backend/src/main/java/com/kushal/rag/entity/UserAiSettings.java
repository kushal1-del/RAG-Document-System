package com.kushal.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_ai_settings")
public class UserAiSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationMode generationMode = GenerationMode.MY_API_KEY;

    /**
     * ID of the user's selected API key.
     * Nullable because the user may not have configured
     * a personal API key yet.
     */
    private String selectedApiKeyId;

    /**
     * Free trial configuration.
     *
     * Every user gets 5 free answer-generation calls
     * using the application's API key.
     */
    @Column(nullable = false)
    private int freeCallsAllowed = 5;

    @Column(nullable = false)
    private int freeCallsUsed = 0;

    public UserAiSettings() {
    }

    public UserAiSettings(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public GenerationMode getGenerationMode() {
        return generationMode;
    }

    public void setGenerationMode(GenerationMode generationMode) {
        this.generationMode = generationMode;
    }

    public String getSelectedApiKeyId() {
        return selectedApiKeyId;
    }

    public void setSelectedApiKeyId(String selectedApiKeyId) {
        this.selectedApiKeyId = selectedApiKeyId;
    }

    public int getFreeCallsAllowed() {
        return freeCallsAllowed;
    }

    public void setFreeCallsAllowed(int freeCallsAllowed) {
        this.freeCallsAllowed = freeCallsAllowed;
    }

    public int getFreeCallsUsed() {
        return freeCallsUsed;
    }

    public void setFreeCallsUsed(int freeCallsUsed) {
        this.freeCallsUsed = freeCallsUsed;
    }
}