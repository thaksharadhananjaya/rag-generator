package com.rag.raggeneratorapplication.rag.controller;

import com.rag.raggeneratorapplication.rag.dto.QueryRequest;
import com.rag.raggeneratorapplication.rag.dto.QueryResponse;
import com.rag.raggeneratorapplication.rag.service.RagService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Retrieval-Augmented Generation (RAG) queries.
 *
 * <p>Accepts questions scoped to a specific knowledge base and delegates
 * answer generation to {@link RagService}.
 *
 * @author Thakshara Dhananjaya
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/query")
public class RagController {

    private final RagService ragService;

    @PostMapping
    public QueryResponse query(@PathVariable UUID knowledgeBaseId, @Valid @RequestBody QueryRequest request) {
        return ragService.answer(knowledgeBaseId, request);
    }
}
