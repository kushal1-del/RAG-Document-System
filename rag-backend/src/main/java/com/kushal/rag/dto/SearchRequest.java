package com.kushal.rag.dto;

import jakarta.validation.constraints.NotBlank;

public class SearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    private String documentId; // optional — null means search all documents

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}