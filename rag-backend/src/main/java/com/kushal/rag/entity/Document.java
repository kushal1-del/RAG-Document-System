package com.kushal.rag.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class Document {

	@Id
	private String id;

	private String fileName;
	private String storageKey;
	private String status;

	// ID of the User who uploaded this document.
	// Used to scope search/ask/list operations so users
	// only ever see their own documents.
	private String ownerId;

	public Document() {
	}

	public Document(String id, String fileName, String storageKey, String status, String ownerId) {
		this.id = id;
		this.fileName = fileName;
		this.storageKey = storageKey;
		this.status = status;
		this.ownerId = ownerId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public void setStorageKey(String storageKey) {
		this.storageKey = storageKey;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
}