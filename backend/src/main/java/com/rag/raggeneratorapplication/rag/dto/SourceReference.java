package com.rag.raggeneratorapplication.rag.dto;

import com.rag.raggeneratorapplication.port.vector.VectorMatch;
import java.util.UUID;

/**
 * A chunk that grounded the answer, with enough detail for the caller to trace it
 * back to a document (fetch full document metadata via {@code GET /api/v1/documents/{id}}).
 *
 * @param chunkId    id of the retrieved chunk
 * @param documentId document the chunk belongs to
 * @param ordinal    chunk position within the document
 * @param page       source page number, or {@code null} if unpaged
 * @param score      cosine similarity to the question in [0,1]
 * @param excerpt    the chunk text, truncated for display
 */
public record SourceReference(
        UUID chunkId,
        UUID documentId,
        int ordinal,
        Integer page,
        double score,
        String excerpt) {

    private static final int EXCERPT_LIMIT = 300;

    public static SourceReference from(VectorMatch match) {
        String text = match.text() == null ? "" : match.text().strip();
        String excerpt = text.length() <= EXCERPT_LIMIT ? text : text.substring(0, EXCERPT_LIMIT) + "…";
        double score = Math.round(match.score() * 10_000d) / 10_000d;
        return new SourceReference(match.chunkId(), match.documentId(), match.ordinal(), match.page(), score, excerpt);
    }
}
