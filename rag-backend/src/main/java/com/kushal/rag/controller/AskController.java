package com.kushal.rag.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kushal.rag.dto.AskRequest;
import com.kushal.rag.service.RagService;

import jakarta.validation.Valid;

@RestController
public class AskController {

   // private final RagService_old ragService;

//    public AskController(RagService_old ragService) {
//        this.ragService = ragService;
//    }

	private final RagService ragService;
	
	public AskController(RagService ragService) {
		this.ragService = ragService;
	}
	
	
    @PostMapping("/api/ask")
    public String ask(@Valid @RequestBody AskRequest request) {

        return ragService.ask(request.getQuestion(), request.getDocumentId());
    }
}