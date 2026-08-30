package com.rag.raggeneratorapplication.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning knobs for RAG retrieval, applied by the rag module.
 *
 * @param topK             maximum chunks to retrieve per query
 * @param minScore         cosine-similarity floor (0..1); chunks below this are discarded
 */
@ConfigurationProperties(prefix = "app.retrieval")
public record RetrievalProperties(
        int topK,
        double minScore) {

    public RetrievalProperties {
        if (topK <= 0) {
            topK = 5;
        }
        if (minScore <= 0 || minScore > 1) {
            minScore = 0.70;
        }
    }
}
