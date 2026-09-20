package com.kushal.rag.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kushal.rag.entity.User;
import com.kushal.rag.model.SearchResult;
import com.kushal.rag.service.chat.GeminiChatService_old;
import com.kushal.rag.service.embedding.EmbeddingService;
import com.kushal.rag.service.vector.QdrantSearchService;

@Service
public class RagService_old {

	private final EmbeddingService embeddingService;
	private final QdrantSearchService qdrantSearchService;
	private final GeminiChatService_old geminiChatService;
	private final CurrentUserService currentUserService;

	@Value("${rag.search.default-limit}")
	private int defaultLimit;

	public RagService_old(EmbeddingService embeddingService, QdrantSearchService qdrantSearchService,
			GeminiChatService_old geminiChatService, CurrentUserService currentUserService) {
		this.embeddingService = embeddingService;
		this.qdrantSearchService = qdrantSearchService;
		this.geminiChatService = geminiChatService;
		this.currentUserService = currentUserService;
	}

	public String ask(String question, String documentId) {

		User currentUser = currentUserService.getCurrentUser();

		// 1. Embed the question
		float[] queryEmbedding = embeddingService.generateEmbedding(question);

		// 2. Search Qdrant for this user's chunks (optionally scoped to one document)
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

		// 5. Get the answer from Gemini
		return geminiChatService.generateAnswer(prompt);
	}
}