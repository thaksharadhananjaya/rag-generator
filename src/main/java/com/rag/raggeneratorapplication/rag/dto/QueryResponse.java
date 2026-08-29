package com.rag.raggeneratorapplication.rag.dto;

import java.util.List;

/**
 * Answer to a query, grounded in retrieved chunks.
 *
 * @param answer           the generated answer
 * @param sources          chunks used to ground the answer (empty when nothing was retrieved)
 * @param model            the LLM model that produced the answer, or {@code null} if the LLM was not called
 * @param tokensUsed       total tokens billed for the generation, or {@code null} if unknown / not called
 * @param retrievedChunks  number of chunks that cleared the similarity threshold
 */
public record QueryResponse(
        String answer,
        List<SourceReference> sources,
        String model,
        Integer tokensUsed,
        int retrievedChunks) {

    /** Response for when retrieval found nothing to ground an answer; the LLM is not called. */
    public static QueryResponse notGrounded(String answer) {
        return new QueryResponse(answer, List.of(), null, null, 0);
    }
}
