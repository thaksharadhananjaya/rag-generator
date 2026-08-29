package com.rag.raggeneratorapplication.rag.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/v1/knowledge-bases/{id}/query}.
 *
 * @param question the natural-language question
 * @param topK     optional override for the number of chunks to retrieve
 * @param minScore optional override for the cosine-similarity floor (0..1)
 */
public record QueryRequest(
        @NotBlank
        @Size(max = 4000)
        String question,

        @Positive
        @Max(50)
        Integer topK,

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        Double minScore) {
}
