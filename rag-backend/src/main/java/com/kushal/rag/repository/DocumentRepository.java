package com.kushal.rag.repository;

import java.util.List;

import com.kushal.rag.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findByOwnerId(String ownerId);
}