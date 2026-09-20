package com.kushal.rag.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kushal.rag.entity.GenerationMode;
import com.kushal.rag.entity.User;
import com.kushal.rag.entity.UserAiSettings;
import com.kushal.rag.model.SearchResult;
import com.kushal.rag.service.chat.GeminiChatService;
import com.kushal.rag.service.embedding.EmbeddingService;
import com.kushal.rag.service.vector.QdrantSearchService;

@Service
public class RagService {

	private final EmbeddingService embeddingService;
	private final QdrantSearchService qdrantSearchService;
	private final GeminiChatService geminiChatService;
	private final CurrentUserService currentUserService;
	private final UserApiKeyService userApiKeyService;

	@Value("${rag.search.default-limit}")
	private int defaultLimit;

	public RagService(EmbeddingService embeddingService, QdrantSearchService qdrantSearchService,
			GeminiChatService geminiChatService, CurrentUserService currentUserService,
			UserApiKeyService userApiKeyService) {

		this.embeddingService = embeddingService;
		this.qdrantSearchService = qdrantSearchService;
		this.geminiChatService = geminiChatService;
		this.currentUserService = currentUserService;
		this.userApiKeyService = userApiKeyService;
	}

	public String ask(String question, String documentId) {

		User currentUser = currentUserService.getCurrentUser();

		// Get the user's AI generation settings.
		UserAiSettings settings = userApiKeyService.getOrCreateSettings(currentUser.getId());

		// 1. Embed the question
		float[] queryEmbedding = embeddingService.generateEmbedding(question);

		// 2. Search Qdrant for this user's chunks
		// (optionally scoped to one document)
		List<SearchResult> results = qdrantSearchService.search(question, queryEmbedding, defaultLimit,
				currentUser.getId(), documentId);

		// 3. Build context text from retrieved chunks
		StringBuilder contextBuilder = new StringBuilder();

		for (SearchResult result : results) {
			contextBuilder.append(result.getContent());
			contextBuilder.append("\n\n");
		}

		String context = contextBuilder.toString();

		// 4. Build the final prompt
		String prompt = "You are a helpful assistant answering questions " + "based only on the provided context.\n\n"
				+ "Context:\n" + context + "\n\nQuestion: " + question + "\n\nAnswer using only the context above. "
				+ "If the answer is not in the context, say you don't know.";

		// 5. Generate the answer using the selected generation mode.
		if (settings.getGenerationMode() == GenerationMode.FREE_TRIAL) {

			if (settings.getFreeCallsUsed() >= settings.getFreeCallsAllowed()) {

				throw new RuntimeException("Free trial limit reached. Please configure your own API key.");
			}

			String answer = geminiChatService.generateAnswer(prompt);

			// Consume one free call only after successful answer generation.
			settings.setFreeCallsUsed(settings.getFreeCallsUsed() + 1);

			// Save the updated free-call usage.
			userApiKeyService.saveSettings(settings);

			return answer;
		}

		if (settings.getGenerationMode() == GenerationMode.MY_API_KEY) {

			String userApiKey = userApiKeyService.getDecryptedSelectedApiKey();

			return geminiChatService.generateAnswer(prompt, userApiKey);
		}

		throw new RuntimeException("Unsupported generation mode.");
	}
}