package com.kushal.rag.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kushal.rag.entity.GenerationMode;
import com.kushal.rag.entity.User;
import com.kushal.rag.entity.UserAiSettings;
import com.kushal.rag.service.CurrentUserService;
import com.kushal.rag.service.UserApiKeyService;

@RestController
@RequestMapping("/api/ai/settings")
public class UserAiSettingsController {

    private final UserApiKeyService userApiKeyService;
    private final CurrentUserService currentUserService;

    public UserAiSettingsController(
            UserApiKeyService userApiKeyService,
            CurrentUserService currentUserService) {

        this.userApiKeyService = userApiKeyService;
        this.currentUserService = currentUserService;
    }

    /**
     * Returns the AI settings of the currently authenticated user.
     */
    @GetMapping
    public ResponseEntity<UserAiSettings> getSettings() {

        User currentUser = currentUserService.getCurrentUser();

        UserAiSettings settings =
                userApiKeyService.getOrCreateSettings(
                        currentUser.getId());

        return ResponseEntity.ok(settings);
    }

    /**
     * Changes the answer-generation mode.
     *
     * FREE_TRIAL:
     * Uses the application's Gemini API key and consumes
     * the user's available free calls.
     *
     * MY_API_KEY:
     * Uses the user's selected API key.
     */
    @PutMapping("/mode")
    public ResponseEntity<UserAiSettings> updateGenerationMode(
            @RequestBody GenerationModeRequest request) {

        User currentUser = currentUserService.getCurrentUser();

        UserAiSettings settings =
                userApiKeyService.getOrCreateSettings(
                        currentUser.getId());

        if (request.getGenerationMode() == null) {
            throw new IllegalArgumentException(
                    "Generation mode is required.");
        }

        if (request.getGenerationMode() == GenerationMode.MY_API_KEY) {

            /*
             * User cannot switch to personal API key mode
             * without having a valid selected API key.
             */
            userApiKeyService.getSelectedApiKey();
        }

        settings.setGenerationMode(
                request.getGenerationMode());

        UserAiSettings savedSettings =
                userApiKeyService.saveSettings(settings);

        return ResponseEntity.ok(savedSettings);
    }

    public static class GenerationModeRequest {

        private GenerationMode generationMode;

        public GenerationMode getGenerationMode() {
            return generationMode;
        }

        public void setGenerationMode(
                GenerationMode generationMode) {

            this.generationMode = generationMode;
        }
    }
}