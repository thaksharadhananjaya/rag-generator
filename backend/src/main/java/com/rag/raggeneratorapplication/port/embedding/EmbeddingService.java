package com.rag.raggeneratorapplication.port.embedding;

import java.util.List;

/**
 * Application-owned abstraction over a text embedding provider.
 *
 * <p>The adapter is responsible for provider batching limits, authentication,
 * timeouts and retries. {@link #modelId()} and {@link #dimension()} let callers
 * record which model produced stored vectors and reject queries that would mix
 * incompatible models.
 *
 * @author Thakshara Dhananjaya
 */
public interface EmbeddingService {

    /**
     * Embeds a batch of texts, preserving order.
     *
     * @throws com.rag.raggeneratorapplication.exception.DependencyUnavailableException
     *         if the provider fails or is unreachable after the adapter's retries
     */
    EmbeddingResult embed(List<String> texts);

    /** Convenience for embedding a single text (e.g. a query). */
    default float[] embedOne(String text) {
        return embed(List.of(text)).vectors().getFirst();
    }

    /** Identifier of the model this adapter is currently configured to use. */
    String modelId();

    /** Vector length produced by {@link #modelId()}. */
    int dimension();
}
