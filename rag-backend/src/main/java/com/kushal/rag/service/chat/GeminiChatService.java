package com.kushal.rag.service.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

@Service
public class GeminiChatService {

    private final String applicationApiKey;
    private final String chatModel;

    public GeminiChatService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.chat.model}") String chatModel) {

        this.applicationApiKey = apiKey;
        this.chatModel = chatModel;
    }

    /**
     * Generates an answer using the application's Gemini API key.
     *
     * This is used for the user's free trial calls.
     */
    public String generateAnswer(String prompt) {

        return generateAnswer(prompt, applicationApiKey);
    }

    /**
     * Generates an answer using the supplied Gemini API key.
     *
     * The supplied key can be the user's decrypted API key.
     */
    public String generateAnswer(String prompt, String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini API key is required.");
        }

        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        GenerateContentResponse response =
                client.models.generateContent(
                        chatModel,
                        prompt,
                        GenerateContentConfig.builder().build());

        String text = response.text();

        if (text == null || text.isBlank()) {
            throw new RuntimeException(
                    "Gemini returned an empty response.");
        }

        return text;
    }
}