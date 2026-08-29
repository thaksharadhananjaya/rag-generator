package com.rag.raggeneratorapplication.port.embedding;

import java.util.List;

/**
 * Embedding vectors for a batch of input texts, in the same order as the input.
 *
 * <p>{@code model} and {@code dimension} are carried on the result because an
 * embedding is only comparable to other embeddings from the same model: they are
 * persisted alongside each vector and checked at query time.
 *
 * @param vectors     one vector per input text; every vector has length {@code dimension}
 * @param model       provider model id that produced the vectors
 * @param dimension   length of each vector
 * @param totalTokens tokens billed for the batch, or {@code null} if not reported
 */
public record EmbeddingResult(
        List<float[]> vectors,
        String model,
        int dimension,
        Integer totalTokens) {

    public EmbeddingResult {
        vectors = List.copyOf(vectors);
    }
}
