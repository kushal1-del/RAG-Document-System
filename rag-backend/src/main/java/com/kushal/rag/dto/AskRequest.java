package com.kushal.rag.dto;

import jakarta.validation.constraints.NotBlank;

public class AskRequest {

    @NotBlank(message = "Question is required")
    private String question;

    private String documentId; // optional — null means search all documents

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}