package com.kushal.rag.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kushal.rag.entity.Document;
import com.kushal.rag.entity.User;
import com.kushal.rag.service.CurrentUserService;
import com.kushal.rag.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

	private final DocumentService documentService;
	private final CurrentUserService currentUserService;

	public DocumentController(DocumentService documentService, CurrentUserService currentUserService) {
		this.documentService = documentService;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/upload")
	public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {

		try {

			Document document = documentService.uploadDocument(file);

			return ResponseEntity.status(HttpStatus.CREATED).body(document);

		} catch (IOException e) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to upload document: " + e.getMessage());
		}
	}

	@GetMapping("/{documentId}")
	public ResponseEntity<Document> getDocument(@PathVariable String documentId) {

		Document document = documentService.getDocumentById(documentId);

		return ResponseEntity.ok(document);
	}

	@GetMapping
	public List<Document> listMyDocuments() {

		User currentUser = currentUserService.getCurrentUser();

		return documentService.getMyDocuments(currentUser.getId());
	}
	
	@DeleteMapping("/{documentId}")
	public ResponseEntity<String> deleteDocument(@PathVariable String documentId) {

	    User currentUser = currentUserService.getCurrentUser();

	    documentService.deleteMyDocument(documentId, currentUser.getId());

	    return ResponseEntity.ok("Document deleted successfully.");
	}
}