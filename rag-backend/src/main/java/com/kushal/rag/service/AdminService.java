package com.kushal.rag.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kushal.rag.dto.DocumentSummaryResponse;
import com.kushal.rag.dto.PagedResponse;
import com.kushal.rag.dto.UserSummaryResponse;
import com.kushal.rag.entity.Document;
import com.kushal.rag.entity.Role;
import com.kushal.rag.entity.User;
import com.kushal.rag.exception.ResourceNotFoundException;
import com.kushal.rag.repository.DocumentRepository;
import com.kushal.rag.repository.UserRepository;
import com.kushal.rag.service.vector.QdrantVectorService;

@Service
public class AdminService {
	private static final Logger log = LoggerFactory.getLogger(AdminService.class);

	private final UserRepository userRepository;
	private final DocumentRepository documentRepository;
	private final PasswordEncoder passwordEncoder;

	private final QdrantVectorService qdrantVectorService;

	public AdminService(UserRepository userRepository, DocumentRepository documentRepository,
			PasswordEncoder passwordEncoder, QdrantVectorService qdrantVectorService) {
		this.userRepository = userRepository;
		this.documentRepository = documentRepository;
		this.passwordEncoder = passwordEncoder;
		this.qdrantVectorService = qdrantVectorService;
	}

	public PagedResponse<UserSummaryResponse> listAllUsers(int page, int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<User> userPage = userRepository.findAll(pageable);

		List<UserSummaryResponse> content = userPage.getContent().stream()
				.map(user ->new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isEnabled())).toList();

		return new PagedResponse<>(content, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements(),
				userPage.getTotalPages());
	}

	public PagedResponse<DocumentSummaryResponse> listAllDocuments(int page, int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<Document> documentPage = documentRepository.findAll(pageable);

		List<DocumentSummaryResponse> content = documentPage.getContent().stream()
				.map(document -> new DocumentSummaryResponse(document.getId(), document.getFileName(),
						document.getStatus(), document.getOwnerId()))
				.toList();

		return new PagedResponse<>(content, documentPage.getNumber(), documentPage.getSize(),
				documentPage.getTotalElements(), documentPage.getTotalPages());
	}

	public UserSummaryResponse createUser(String email, String rawPassword, String roleString) {

		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("A user with this email already exists.");
		}

		Role role;

		try {
			role = Role.valueOf(roleString.toUpperCase());

		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid role: " + roleString + ". Must be USER or ADMIN.");
		}

		String hashedPassword = passwordEncoder.encode(rawPassword);

		User user = new User(email, hashedPassword, role);
		user = userRepository.save(user);

		return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isEnabled());
	}

	public UserSummaryResponse disableUser(String userId) {

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

		user.setEnabled(false);
		user = userRepository.save(user);

		return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isEnabled());
	}

	public void hardDeleteUser(String userId) {

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

		// 1. Delete this user's documents from Postgres
		List<Document> documents = documentRepository.findAll().stream()
				.filter(document -> userId.equals(document.getOwnerId())).toList();

		documentRepository.deleteAll(documents);

		// 2. Delete this user's vectors from Qdrant
		qdrantVectorService.deleteByOwnerId(userId);

		// 3. Delete the user row itself
		userRepository.delete(user);

		log.info("Hard-deleted user {} and all associated data.", user.getEmail());
	}
	public UserSummaryResponse enableUser(String userId) {

	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

	    user.setEnabled(true);
	    user = userRepository.save(user);
	    log.info("Enabled user {}.", user.getEmail());
	    log.info("User {} is now enabled: {}", user.getEmail(), user.isEnabled());

	    return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isEnabled());
	}
}