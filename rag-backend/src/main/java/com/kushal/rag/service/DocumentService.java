package com.kushal.rag.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kushal.rag.entity.Document;
import com.kushal.rag.entity.User;
import com.kushal.rag.exception.ResourceNotFoundException;
import com.kushal.rag.model.DocumentChunk;
import com.kushal.rag.repository.DocumentRepository;
import com.kushal.rag.service.chunking.TextChunker;
import com.kushal.rag.service.embedding.EmbeddingService;
import com.kushal.rag.service.parsing.DocumentParser;
import com.kushal.rag.service.storage.FileStorageService;
import com.kushal.rag.service.vector.QdrantVectorService;

@Service
public class DocumentService {

	private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
	private final FileStorageService fileStorageService;
	private final DocumentRepository documentRepository;
	private final DocumentParser documentParser;
	private final TextChunker textChunker;
	private final EmbeddingService embeddingService;
	private final QdrantVectorService qdrantVectorService;
	private final CurrentUserService currentUserService;

	public DocumentService(DocumentRepository documentRepository, FileStorageService fileStorageService,
			DocumentParser documentParser, TextChunker textChunker, EmbeddingService embeddingService,
			QdrantVectorService qdrantVectorService, CurrentUserService currentUserService) {

		this.documentRepository = documentRepository;
		this.fileStorageService = fileStorageService;
		this.documentParser = documentParser;
		this.textChunker = textChunker;
		this.embeddingService = embeddingService;
		this.qdrantVectorService = qdrantVectorService;
		this.currentUserService = currentUserService;
	}

	public Document uploadDocument(MultipartFile file) throws IOException {

		// 1. Generate unique document ID
		String documentId = UUID.randomUUID().toString();

		// 1a. Identify the logged-in user uploading this document
		User currentUser = currentUserService.getCurrentUser();
		String ownerId = currentUser.getId();

		// 2. Store the actual file
		String storageKey = fileStorageService.store(file, documentId);

		// 3. Create document metadata, tagged with the uploader's ID
		Document document = new Document(documentId, file.getOriginalFilename(), storageKey, "UPLOADED", ownerId);

		// 4. Save metadata in PostgreSQL
		document = documentRepository.save(document);

		// 5. Retrieve the stored file
		try (InputStream inputStream = fileStorageService.retrieve(storageKey)) {

			// 6. Parse file → text (format detected from filename extension)
			String extractedText = documentParser.parse(file.getOriginalFilename(), inputStream);

			// 7. Text → DocumentChunks
			List<DocumentChunk> chunks = textChunker.chunk(documentId, extractedText);

			log.info("Total chunks: {}", chunks.size());

			// 7a. Extract chunk text and generate embeddings using
			// Gemini's BATCH embedding API — groups multiple chunks
			// into fewer API calls (EmbeddingService handles batching
			// and concurrency internally).
			List<String> chunkTexts = chunks.stream().map(DocumentChunk::getContent).toList();

			List<float[]> embeddings = embeddingService.generateEmbeddings(chunkTexts);

			log.info("Generated {} embeddings for {} chunks.", embeddings.size(), chunks.size());

			// 7b. Store ALL chunks in ONE batched Qdrant operation
			// (internally split into sub-batches of 100), instead
			// of one call per chunk.
			qdrantVectorService.storeChunks(chunks, embeddings, ownerId);

			log.info("========== END CHUNKS ==========");
		}

		return document;
	}

	public Document getDocumentById(String documentId) {
		return documentRepository.findById(documentId)
				.orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
	}

	public List<Document> getMyDocuments(String ownerId) {
		return documentRepository.findByOwnerId(ownerId);
	}

	public void deleteMyDocument(String documentId, String ownerId) {

		Document document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

		if (!document.getOwnerId().equals(ownerId)) {
			throw new IllegalArgumentException("You do not have permission to delete this document.");
		}

		documentRepository.delete(document);
		qdrantVectorService.deleteByDocumentId(documentId);

		log.info("User {} deleted document {}", ownerId, documentId);
	}
}