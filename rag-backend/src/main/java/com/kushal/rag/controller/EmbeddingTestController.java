package com.kushal.rag.controller;

import com.kushal.rag.service.embedding.EmbeddingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmbeddingTestController {

    private final EmbeddingService embeddingService;

    public EmbeddingTestController(
            EmbeddingService embeddingService) {

        this.embeddingService = embeddingService;
    }

    @GetMapping("/api/test/embedding")
    public String testEmbedding(
            @RequestParam String text) {

        float[] vector =
                embeddingService.generateEmbedding(text);

        return "Embedding generated successfully. "
                + "Dimensions: " + vector.length;
    }
}