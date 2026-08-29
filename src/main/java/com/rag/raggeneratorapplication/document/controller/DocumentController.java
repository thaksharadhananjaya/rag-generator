package com.rag.raggeneratorapplication.document.controller;

import com.rag.raggeneratorapplication.common.page.PageResponse;
import com.rag.raggeneratorapplication.document.dto.DocumentResponse;
import com.rag.raggeneratorapplication.document.entity.Document;
import com.rag.raggeneratorapplication.document.service.DocumentService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService service;

    @PostMapping(path = "/knowledge-bases/{knowledgeBaseId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@PathVariable UUID knowledgeBaseId,
                                                   @RequestParam("file") MultipartFile file,
                                                   UriComponentsBuilder uriBuilder) {
        Document document = service.upload(knowledgeBaseId, file);
        URI location = uriBuilder.path("/api/v1/documents/{id}").buildAndExpand(document.getId()).toUri();
        return ResponseEntity.accepted().location(location).body(DocumentResponse.from(document));
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public PageResponse<DocumentResponse> list(
            @PathVariable UUID knowledgeBaseId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(service.getAll(knowledgeBaseId, pageable), DocumentResponse::from);
    }

    @GetMapping("/documents/{documentId}")
    public DocumentResponse get(@PathVariable UUID documentId) {
        return DocumentResponse.from(service.get(documentId));
    }

    @PostMapping("/documents/{documentId}/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentResponse reingest(@PathVariable UUID documentId) {
        return DocumentResponse.from(service.reingest(documentId));
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID documentId) {
        service.delete(documentId);
    }
}
