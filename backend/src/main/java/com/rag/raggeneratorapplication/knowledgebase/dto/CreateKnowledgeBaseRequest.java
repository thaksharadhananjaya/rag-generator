package com.rag.raggeneratorapplication.knowledgebase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/v1/knowledge-bases}.
 *
 * @param name        human-readable name, unique (case-insensitive)
 * @param description  optional free text
 */
public record CreateKnowledgeBaseRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 4000)
        String description) {
}
