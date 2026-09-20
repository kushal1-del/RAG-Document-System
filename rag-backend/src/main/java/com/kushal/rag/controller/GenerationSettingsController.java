//package com.kushal.rag.controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.kushal.rag.entity.GenerationMode;
//import com.kushal.rag.entity.User;
//import com.kushal.rag.entity.UserAiSettings;
//import com.kushal.rag.service.CurrentUserService;
//import com.kushal.rag.service.UserApiKeyService;
//
//@RestController
//@RequestMapping("/api/ai/settings")
//public class GenerationSettingsController {
//
//    private final UserApiKeyService userApiKeyService;
//    private final CurrentUserService currentUserService;
//
//    public GenerationSettingsController(
//            UserApiKeyService userApiKeyService,
//            CurrentUserService currentUserService) {
//
//        this.userApiKeyService = userApiKeyService;
//        this.currentUserService = currentUserService;
//    }
//
//    /**
//     * Returns the current AI generation settings
//     * of the authenticated user.
//     */
//    @GetMapping
//    public ResponseEntity<UserAiSettings> getSettings() {
//
//        User currentUser = currentUserService.getCurrentUser();
//
//        UserAiSettings settings =
//                userApiKeyService.getOrCreateSettings(
//                        currentUser.getId());
//
//        return ResponseEntity.ok(settings);
//    }
//
//    /**
//     * Changes the answer-generation mode.
//     *
//     * FREE_TRIAL uses the application's Gemini API key.
//     * MY_API_KEY uses the user's selected API key.
//     */
//    @PutMapping("/generationMode")
//    public ResponseEntity<UserAiSettings> updateGenerationMode(
//            @RequestBody GenerationModeRequest request) {
//
//        if (request == null || request.getGenerationMode() == null) {
//            throw new IllegalArgumentException(
//                    "Generation mode is required.");
//        }
//
//        User currentUser =
//                currentUserService.getCurrentUser();
//
//        UserAiSettings settings =
//                userApiKeyService.getOrCreateSettings(
//                        currentUser.getId());
//
//        settings.setGenerationMode(
//                request.getGenerationMode());
//
//        UserAiSettings savedSettings =
//                userApiKeyService.saveSettings(settings);
//
//        return ResponseEntity.ok(savedSettings);
//    }
//
//    public static class GenerationModeRequest {
//
//        private GenerationMode generationMode;
//
//        public GenerationMode getGenerationMode() {
//            return generationMode;
//        }
//
//        public void setGenerationMode(
//                GenerationMode generationMode) {
//
//            this.generationMode = generationMode;
//        }
//    }
//}