package com.rag.raggeneratorapplication.port.vector;

import java.util.List;
import java.util.UUID;

/**
 * Application-owned abstraction over the vector database. This port <em>owns
 * chunk persistence</em>: chunk text, provenance and embedding are stored and
 * returned together, so retrieval is a single scoped query with no join back to
 * other modules.
 *
 * <p>Initial implementation is PostgreSQL + pgvector ({@code PgVectorStore}).
 * Every operation is scoped by {@code knowledgeBaseId} so chunks from unrelated
 * document sets can never be read or deleted together.
 */
public interface VectorStore {

    /**
     * Replaces all chunks for a document with the given set (delete-then-insert),
     * making re-ingestion idempotent. Callers pass the full chunk list for the
     * document, not a delta.
     */
    void replaceDocumentChunks(UUID knowledgeBaseId, UUID documentId, List<VectorChunk> chunks);

    /** Removes every chunk belonging to the document. No-op if there are none. */
    void deleteByDocument(UUID knowledgeBaseId, UUID documentId);

    /** Removes every chunk in the knowledge base. Used when a knowledge base is deleted. */
    void deleteByKnowledgeBase(UUID knowledgeBaseId);

    /**
     * Returns up to {@code topK} chunks in the query's knowledge base whose
     * similarity to {@link VectorSearchQuery#queryEmbedding()} is at least
     * {@link VectorSearchQuery#minScore()}, most similar first.
     */
    List<VectorMatch> search(VectorSearchQuery query);
}
