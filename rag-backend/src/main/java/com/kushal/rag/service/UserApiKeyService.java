package com.kushal.rag.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kushal.rag.entity.ApiKeyProvider;
import com.kushal.rag.entity.User;
import com.kushal.rag.entity.UserAiSettings;
import com.kushal.rag.entity.UserApiKey;
import com.kushal.rag.repository.UserAiSettingsRepository;
import com.kushal.rag.repository.UserApiKeyRepository;
import com.kushal.rag.service.security.EncryptionService;

@Service
public class UserApiKeyService {

    private static final Logger log =
            LoggerFactory.getLogger(UserApiKeyService.class);

    private final UserApiKeyRepository userApiKeyRepository;
    private final UserAiSettingsRepository userAiSettingsRepository;
    private final CurrentUserService currentUserService;
    private final EncryptionService encryptionService;

    public UserApiKeyService(
            UserApiKeyRepository userApiKeyRepository,
            UserAiSettingsRepository userAiSettingsRepository,
            CurrentUserService currentUserService,
            EncryptionService encryptionService) {

        this.userApiKeyRepository = userApiKeyRepository;
        this.userAiSettingsRepository = userAiSettingsRepository;
        this.currentUserService = currentUserService;
        this.encryptionService = encryptionService;
    }

    /**
     * Adds a new API key for the currently authenticated user.
     *
     * The actual API key is encrypted before it is stored.
     */
    public UserApiKey addApiKey(
            ApiKeyProvider provider,
            String label,
            String apiKey,
            LocalDateTime expiresAt) {

        User currentUser = currentUserService.getCurrentUser();

        validateApiKeyInput(provider, label, apiKey, expiresAt);

        String encryptedKey = encryptionService.encrypt(apiKey);

        UserApiKey userApiKey = new UserApiKey(
                currentUser.getId(),
                provider,
                label,
                encryptedKey,
                expiresAt);

        UserApiKey savedKey = userApiKeyRepository.save(userApiKey);

        log.info(
                "API key added successfully for userId={}, provider={}, keyId={}, expiresAt={}",
                currentUser.getId(),
                provider,
                savedKey.getId(),
                expiresAt);

        return savedKey;
    }

    /**
     * Returns all API keys belonging to the currently authenticated user.
     *
     * The encrypted API key must never be exposed to the frontend.
     */
    public List<UserApiKey> getMyApiKeys() {

        User currentUser = currentUserService.getCurrentUser();

        List<UserApiKey> keys =
                userApiKeyRepository.findByUserId(currentUser.getId());

        log.info(
                "Retrieved {} API keys for userId={}",
                keys.size(),
                currentUser.getId());

        return keys;
    }

    /**
     * Selects an API key for answer generation.
     *
     * Ownership, enabled status and expiry are checked before
     * the key can become the selected key.
     */
    public void selectApiKey(String apiKeyId) {

        User currentUser = currentUserService.getCurrentUser();

        UserApiKey apiKey = userApiKeyRepository
                .findByIdAndUserId(apiKeyId, currentUser.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "API key not found for current user."));

        validateUsableApiKey(apiKey);

        UserAiSettings settings =
                getOrCreateSettings(currentUser.getId());

        settings.setSelectedApiKeyId(apiKey.getId());

        userAiSettingsRepository.save(settings);

        log.info(
                "API key selected for answer generation: userId={}, keyId={}, provider={}",
                currentUser.getId(),
                apiKey.getId(),
                apiKey.getProvider());
    }

    /**
     * Disables an API key belonging to the currently authenticated user.
     *
     * Disabled keys cannot be used for answer generation.
     */
    public void disableApiKey(String apiKeyId) {

        User currentUser = currentUserService.getCurrentUser();

        UserApiKey apiKey = userApiKeyRepository
                .findByIdAndUserId(apiKeyId, currentUser.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "API key not found for current user."));

        apiKey.setEnabled(false);

        userApiKeyRepository.save(apiKey);

        log.info(
                "API key disabled: userId={}, keyId={}",
                currentUser.getId(),
                apiKey.getId());
    }

    /**
     * Returns the currently selected API key for the user.
     *
     * The key is validated before being returned.
     */
    public UserApiKey getSelectedApiKey() {

        User currentUser = currentUserService.getCurrentUser();

        UserAiSettings settings =
                getOrCreateSettings(currentUser.getId());

        String selectedApiKeyId =
                settings.getSelectedApiKeyId();

        if (selectedApiKeyId == null || selectedApiKeyId.isBlank()) {
            throw new RuntimeException(
                    "No API key has been selected.");
        }

        UserApiKey apiKey = userApiKeyRepository
                .findByIdAndUserId(
                        selectedApiKeyId,
                        currentUser.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Selected API key was not found."));

        validateUsableApiKey(apiKey);

        return apiKey;
    }

    /**
     * Decrypts the selected API key.
     *
     * This method will later be used by the answer-generation
     * layer when USER_API_KEY mode is enabled.
     */
    public String getDecryptedSelectedApiKey() {

        UserApiKey apiKey = getSelectedApiKey();

        return encryptionService.decrypt(
                apiKey.getEncryptedKey());
    }

    /**
     * Returns the user's AI settings, creating the default
     * settings record if it does not exist yet.
     */
    public UserAiSettings getOrCreateSettings(String userId) {

        return userAiSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {

                    UserAiSettings settings =
                            new UserAiSettings(userId);

                    UserAiSettings savedSettings =
                            userAiSettingsRepository.save(settings);

                    log.info(
                            "Created default AI settings for userId={}",
                            userId);

                    return savedSettings;
                });
    }

    private void validateApiKeyInput(
            ApiKeyProvider provider,
            String label,
            String apiKey,
            LocalDateTime expiresAt) {

        if (provider == null) {
            throw new IllegalArgumentException(
                    "API key provider is required.");
        }

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                    "API key label is required.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "API key is required.");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException(
                    "API key expiry date is required.");
        }

        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "API key expiry date must be in the future.");
        }
    }

    private void validateUsableApiKey(UserApiKey apiKey) {

        if (!apiKey.isEnabled()) {
            throw new RuntimeException(
                    "API key is disabled.");
        }

        if (!apiKey.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new RuntimeException(
                    "API key has expired.");
        }
    }
    public UserAiSettings saveSettings(UserAiSettings settings) {

        UserAiSettings savedSettings =
                userAiSettingsRepository.save(settings);

        log.info(
                "AI settings updated: userId={}, freeCallsUsed={}, generationMode={}",
                settings.getUserId(),
                settings.getFreeCallsUsed(),
                settings.getGenerationMode());

        return savedSettings;
    }
}