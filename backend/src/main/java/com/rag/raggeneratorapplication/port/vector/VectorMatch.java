package com.rag.raggeneratorapplication.port.vector;

import java.util.UUID;

/**
 * One retrieval hit: the stored chunk plus its similarity to the query.
 * Carries enough provenance for the rag module to build a source citation.
 *
 * @param chunkId    id of the matched chunk
 * @param documentId document the chunk belongs to
 * @param ordinal    chunk position within the document
 * @param text       the chunk's text
 * @param page       source page number, or {@code null} if unpaged
 * @param score      cosine similarity in [0,1], higher is closer
 */
public record VectorMatch(
        UUID chunkId,
        UUID documentId,
        int ordinal,
        String text,
        Integer page,
        double score) {
}
