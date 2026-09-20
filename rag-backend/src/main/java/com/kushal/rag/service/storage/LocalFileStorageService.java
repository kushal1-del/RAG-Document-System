package com.kushal.rag.service.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalFileStorageService implements FileStorageService {

	private final Path rootLocation = Paths.get("uploads");

	public LocalFileStorageService() throws IOException {
		Files.createDirectories(rootLocation);
	}

	@Override
	public String store(MultipartFile file, String documentId) throws IOException {

		if (file == null || file.isEmpty()) {
			throw new IOException("Cannot store an empty file");
		}

		String originalFileName = file.getOriginalFilename();

		if (originalFileName == null || originalFileName.isBlank()) {
			throw new IOException("File name is missing");
		}

		Path documentDirectory = rootLocation.resolve(documentId);

		Files.createDirectories(documentDirectory);

		Path destination = documentDirectory.resolve(originalFileName);

		try (InputStream inputStream = file.getInputStream()) {
			Files.copy(inputStream, destination);
		}

		return destination.toString();
	}

	@Override
	public InputStream retrieve(String storageKey) throws IOException {

		Path filePath = Paths.get(storageKey);

		if (!Files.exists(filePath)) {
			throw new IOException("File not found: " + storageKey);
		}

		return Files.newInputStream(filePath);
	}

	@Override
	public void delete(String storageKey) throws IOException {

		Path filePath = Paths.get(storageKey);

		Files.deleteIfExists(filePath);
	}
}