package com.kushal.rag.model;

public class DocumentChunk {

	private String documentId;

	private int chunkIndex;

	private String content;

	public DocumentChunk(String documentId, int chunkIndex, String content) {

		this.documentId = documentId;
		this.chunkIndex = chunkIndex;
		this.content = content;
	}

	public String getDocumentId() {
		return documentId;
	}

	public int getChunkIndex() {
		return chunkIndex;
	}

	public String getContent() {
		return content;
	}
}