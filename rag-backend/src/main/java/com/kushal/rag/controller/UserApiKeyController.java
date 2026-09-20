package com.kushal.rag.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kushal.rag.entity.ApiKeyProvider;
import com.kushal.rag.entity.UserApiKey;
import com.kushal.rag.service.UserApiKeyService;

@RestController
@RequestMapping("/api/ai/keys")
public class UserApiKeyController {

    private final UserApiKeyService userApiKeyService;

    public UserApiKeyController(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
    }

    /**
     * Adds a new API key for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<UserApiKey> addApiKey(
            @RequestBody AddApiKeyRequest request) {

        UserApiKey apiKey = userApiKeyService.addApiKey(
                request.getProvider(),
                request.getLabel(),
                request.getApiKey(),
                request.getExpiresAt());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiKey);
    }

    /**
     * Returns all API keys belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<UserApiKey>> getMyApiKeys() {

        return ResponseEntity.ok(
                userApiKeyService.getMyApiKeys());
    }

    /**
     * Selects an API key for answer generation.
     */
    @PostMapping("/{apiKeyId}/select")
    public ResponseEntity<String> selectApiKey(
            @PathVariable String apiKeyId) {

        userApiKeyService.selectApiKey(apiKeyId);

        return ResponseEntity.ok(
                "API key selected successfully.");
    }

    /**
     * Disables an API key.
     */
    @DeleteMapping("/{apiKeyId}")
    public ResponseEntity<String> disableApiKey(
            @PathVariable String apiKeyId) {

        userApiKeyService.disableApiKey(apiKeyId);

        return ResponseEntity.ok(
                "API key disabled successfully.");
    }

    public static class AddApiKeyRequest {

        private ApiKeyProvider provider;
        private String label;
        private String apiKey;
        private LocalDateTime expiresAt;

        public ApiKeyProvider getProvider() {
            return provider;
        }

        public void setProvider(ApiKeyProvider provider) {
            this.provider = provider;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}