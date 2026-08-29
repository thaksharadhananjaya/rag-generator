package com.rag.raggeneratorapplication.knowledgebase.repository;

import com.rag.raggeneratorapplication.knowledgebase.entity.KnowledgeBase;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {

    boolean existsByNameIgnoreCase(String name);
}
