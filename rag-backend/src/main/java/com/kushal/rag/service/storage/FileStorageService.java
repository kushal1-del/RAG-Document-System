package com.kushal.rag.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

	String store(MultipartFile file, String documentId) throws IOException;

	InputStream retrieve(String storageKey) throws IOException;

	void delete(String storageKey) throws IOException;
}