package com.kushal.rag.dto;

/**
 * What we expose to admins about a document.
 * Includes ownerId so admins can see which user
 * uploaded each document.
 */
public class DocumentSummaryResponse {

    private String id;
    private String fileName;
    private String status;
    private String ownerId;

    public DocumentSummaryResponse(String id, String fileName, String status, String ownerId) {
        this.id = id;
        this.fileName = fileName;
        this.status = status;
        this.ownerId = ownerId;
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStatus() {
        return status;
    }

    public String getOwnerId() {
        return ownerId;
    }
}