package com.rag.raggeneratorapplication.document.repository;

import com.rag.raggeneratorapplication.document.entity.Document;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByKnowledgeBaseId(UUID knowledgeBaseId, Pageable pageable);

    List<Document> findByKnowledgeBaseId(UUID knowledgeBaseId);
}
