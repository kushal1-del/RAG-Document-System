package com.kushal.rag.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kushal.rag.dto.SearchRequest;
import com.kushal.rag.entity.User;
import com.kushal.rag.model.SearchResult;
import com.kushal.rag.service.CurrentUserService;
import com.kushal.rag.service.embedding.EmbeddingService;
import com.kushal.rag.service.vector.QdrantSearchService;

import jakarta.validation.Valid;

@RestController
public class SearchController {

	private final EmbeddingService embeddingService;
	private final QdrantSearchService qdrantSearchService;
	private final CurrentUserService currentUserService;

	@Value("${rag.search.default-limit}")
	private int defaultLimit;

	public SearchController(EmbeddingService embeddingService, QdrantSearchService qdrantSearchService,
			CurrentUserService currentUserService) {
		this.embeddingService = embeddingService;
		this.qdrantSearchService = qdrantSearchService;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/api/search")
	public List<SearchResult> search(@Valid @RequestBody SearchRequest request) {

		User currentUser = currentUserService.getCurrentUser();

		float[] queryEmbedding = embeddingService.generateEmbedding(request.getQuery());

		return qdrantSearchService.search(request.getQuery(), queryEmbedding, defaultLimit, currentUser.getId(),
				request.getDocumentId());
	}
}