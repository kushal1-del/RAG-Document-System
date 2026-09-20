package com.kushal.rag.model;

public class SearchResult {

	private final String documentId;
	private final int chunkIndex;
	private final String content;
	private final float score;

	public SearchResult(String documentId, int chunkIndex, String content, float score) {

		this.documentId = documentId;
		this.chunkIndex = chunkIndex;
		this.content = content;
		this.score = score;
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

	public float getScore() {
		return score;
	}
}