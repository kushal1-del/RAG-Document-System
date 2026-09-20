package com.kushal.rag.service.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

@Service
public class GeminiChatService_old {

	private final Client client;
	private final String chatModel;

	public GeminiChatService_old(@Value("${gemini.api.key}") String apiKey,
			@Value("${gemini.chat.model}") String chatModel) {

		this.client = Client.builder().apiKey(apiKey).build();
		this.chatModel = chatModel;
	}

	public String generateAnswer(String prompt) {

		GenerateContentResponse response = client.models.generateContent(chatModel, prompt,
				GenerateContentConfig.builder().build());

		String text = response.text();

		if (text == null || text.isBlank()) {
			throw new RuntimeException("Gemini returned an empty response.");
		}

		return text;
	}
}