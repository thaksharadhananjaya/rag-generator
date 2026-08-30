package com.rag.raggeneratorapplication.knowledgebase.controller;

import com.rag.raggeneratorapplication.common.page.PageResponse;
import com.rag.raggeneratorapplication.knowledgebase.dto.CreateKnowledgeBaseRequest;
import com.rag.raggeneratorapplication.knowledgebase.dto.KnowledgeBaseResponse;
import com.rag.raggeneratorapplication.knowledgebase.entity.KnowledgeBase;
import com.rag.raggeneratorapplication.knowledgebase.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for Knowledge Base management.
 *
 * <p>Provides endpoints for creating, retrieving, listing, and deleting
 * knowledge bases. Business logic is delegated to {@link KnowledgeBaseService}.
 *
 * @author Thakshara Dhananjaya
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public ResponseEntity<KnowledgeBaseResponse> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        KnowledgeBase created = knowledgeBaseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(KnowledgeBaseResponse.from(created));
    }

    @GetMapping
    public PageResponse<KnowledgeBaseResponse> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(knowledgeBaseService.getAll(pageable), KnowledgeBaseResponse::from);
    }

    @GetMapping("/{id}")
    public KnowledgeBaseResponse get(@PathVariable UUID id) {
        return KnowledgeBaseResponse.from(knowledgeBaseService.get(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        knowledgeBaseService.delete(id);
    }
}
