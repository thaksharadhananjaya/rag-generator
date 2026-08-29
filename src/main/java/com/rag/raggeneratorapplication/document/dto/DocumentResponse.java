package com.rag.raggeneratorapplication.document.dto;

import com.rag.raggeneratorapplication.document.entity.Document;
import com.rag.raggeneratorapplication.document.entity.IngestionStatus;
import java.time.Instant;
import java.util.UUID;

/** API representation of a document and its ingestion state. */
public record DocumentResponse(
        UUID id,
        UUID knowledgeBaseId,
        String filename,
        String contentType,
        long sizeBytes,
        IngestionStatus status,
        String failureReason,
        int retryCount,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt,
        Instant ingestedAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getStatus(),
                document.getFailureReason(),
                document.getRetryCount(),
                document.getChunkCount(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getIngestedAt());
    }
}
