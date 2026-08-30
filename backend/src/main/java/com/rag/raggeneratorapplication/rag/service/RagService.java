package com.rag.raggeneratorapplication.rag.service;

import com.rag.raggeneratorapplication.config.props.RetrievalProperties;
import com.rag.raggeneratorapplication.knowledgebase.service.KnowledgeBaseService;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingService;
import com.rag.raggeneratorapplication.port.llm.LlmResponse;
import com.rag.raggeneratorapplication.port.llm.LlmService;
import com.rag.raggeneratorapplication.port.vector.VectorMatch;
import com.rag.raggeneratorapplication.port.vector.VectorSearchQuery;
import com.rag.raggeneratorapplication.port.vector.VectorStore;
import com.rag.raggeneratorapplication.rag.dto.QueryRequest;
import com.rag.raggeneratorapplication.rag.dto.QueryResponse;
import com.rag.raggeneratorapplication.rag.dto.SourceReference;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Answers questions against a knowledge base:
 * retrieve relevant chunks &rarr; build a grounded prompt &rarr; call the LLM &rarr;
 * return the answer with its sources. Depends only on application ports for the
 * LLM, embeddings and vector store.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private static final String NO_CONTEXT_ANSWER =
            "I couldn't find anything relevant in this knowledge base to answer that.";

    private final KnowledgeBaseService knowledgeBaseService;
    private final PromptService promptService;
    private final LlmService llmService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final RetrievalProperties properties;

    public QueryResponse answer(UUID knowledgeBaseId, QueryRequest request) {
        knowledgeBaseService.get(knowledgeBaseId); // 404 if the knowledge base is unknown
        String question = request.question().strip();

        List<VectorMatch> matches = findMatchingVectors(
                knowledgeBaseId, question, request.topK(), request.minScore());
        if (matches.isEmpty()) {
            log.info("No chunks retrieved for kb={}; skipping LLM call", knowledgeBaseId);
            return QueryResponse.notGrounded(NO_CONTEXT_ANSWER);
        }

        LlmResponse llmResponse = llmService.generate(promptService.buildPrompt(question, matches));

        List<SourceReference> sources = matches.stream().map(SourceReference::from).toList();
        Integer tokensUsed = llmResponse.usage() == null ? null : llmResponse.usage().totalTokens();
        return new QueryResponse(llmResponse.content(), sources, llmResponse.model(), tokensUsed, matches.size());
    }

    private List<VectorMatch> findMatchingVectors(UUID knowledgeBaseId, String question, Integer topK, Double minScore) {
        int effectiveTopK = (topK != null && topK > 0) ? topK : properties.topK();
        double effectiveMinScore = (minScore != null && minScore >= 0) ? minScore : properties.minScore();

        float[] queryEmbedding = embeddingService.embedOne(question);
        List<VectorMatch> matches = vectorStore.search(new VectorSearchQuery(
                knowledgeBaseId, queryEmbedding, embeddingService.modelId(), effectiveTopK, effectiveMinScore));

        log.info("Retrieval for kb={} returned {} chunk(s) (topK={}, minScore={})",
                knowledgeBaseId, matches.size(), effectiveTopK, effectiveMinScore);
        return matches;
    }
}
