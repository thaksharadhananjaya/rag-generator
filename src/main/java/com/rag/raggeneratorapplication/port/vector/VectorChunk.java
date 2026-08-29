package com.rag.raggeneratorapplication.port.vector;

/**
 * A chunk to be persisted by the {@link VectorStore}: its text, provenance and
 * embedding. The store owns chunk persistence, so this carries everything needed
 * to serve a retrieval hit without a join back to the document module.
 *
 * @param ordinal        0-based position of the chunk within its document
 * @param text           the chunk's text
 * @param embedding      the chunk embedding; length must match the store's configured dimension
 * @param embeddingModel model id that produced {@code embedding}
 * @param page           source page number, or {@code null} if unpaged
 * @param startOffset    inclusive start index within the document's full text, or {@code null}
 * @param endOffset      exclusive end index within the document's full text, or {@code null}
 */
public record VectorChunk(
        int ordinal,
        String text,
        float[] embedding,
        String embeddingModel,
        Integer page,
        Integer startOffset,
        Integer endOffset) {
}
