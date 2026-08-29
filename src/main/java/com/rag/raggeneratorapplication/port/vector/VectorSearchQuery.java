package com.rag.raggeneratorapplication.port.vector;

import java.util.UUID;

/**
 * A similarity search request against one knowledge base.
 *
 * @param knowledgeBaseId scope of the search; chunks outside it are never returned
 * @param queryEmbedding  the embedded query
 * @param embeddingModel  model that produced {@code queryEmbedding}; must match the
 *                        model stored for the knowledge base
 * @param topK            maximum number of matches to return
 * @param minScore        cosine-similarity floor in [0,1]; matches below it are dropped
 */
public record VectorSearchQuery(
        UUID knowledgeBaseId,
        float[] queryEmbedding,
        String embeddingModel,
        int topK,
        double minScore) {
}
