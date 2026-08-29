package com.rag.raggeneratorapplication.knowledgebase.dto;

import com.rag.raggeneratorapplication.knowledgebase.entity.KnowledgeBase;
import java.time.Instant;
import java.util.UUID;

/** API representation of a knowledge base. */
public record KnowledgeBaseResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt) {

    public static KnowledgeBaseResponse from(KnowledgeBase kb) {
        return new KnowledgeBaseResponse(
                kb.getId(),
                kb.getName(),
                kb.getDescription(),
                kb.getCreatedAt(),
                kb.getUpdatedAt());
    }
}
