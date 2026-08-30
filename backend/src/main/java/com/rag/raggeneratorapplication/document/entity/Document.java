package com.rag.raggeneratorapplication.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * Metadata and ingestion state for one uploaded file. The original bytes live in
 * object storage under {@link #getStorageKey()}; the derived chunks/vectors are
 * owned by the {@code VectorStore}.
 *
 * <p>The id is assigned by the application before persistence (it is part of the
 * storage key), so this implements {@link Persistable} to keep Spring Data using
 * {@code persist} rather than {@code merge} for new rows.
 *
 * <p>Ingestion state changes go through the {@code mark*} / {@code resetForIngestion}
 * methods, so this entity exposes {@code @Getter} but no {@code @Setter}.
 */
@Entity
@Table(name = "document")
@Getter
public class Document implements Persistable<UUID> {

    private static final int MAX_FAILURE_REASON = 4000;

    @Id
    private UUID id;

    @Getter(AccessLevel.NONE)
    @Transient
    private boolean persisted;

    @Column(name = "knowledge_base_id", nullable = false, updatable = false)
    private UUID knowledgeBaseId;

    @Column(nullable = false, length = 512)
    private String filename;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IngestionStatus status = IngestionStatus.PENDING;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "chunking_config", length = 255)
    private String chunkingConfig;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "ingested_at")
    private Instant ingestedAt;

    protected Document() {
        // for JPA
    }

    public Document(UUID id, UUID knowledgeBaseId, String filename, String contentType, long sizeBytes,
                    String storageKey) {
        this.id = id;
        this.knowledgeBaseId = knowledgeBaseId;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.status = IngestionStatus.PENDING;
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        this.persisted = true;
    }

    public void markProcessing() {
        this.status = IngestionStatus.PROCESSING;
        this.failureReason = null;
    }

    public void markEmbedding() {
        this.status = IngestionStatus.EMBEDDING;
    }

    public void markCompleted(int chunkCount, String chunkingConfig) {
        this.status = IngestionStatus.COMPLETED;
        this.chunkCount = chunkCount;
        this.chunkingConfig = chunkingConfig;
        this.ingestedAt = Instant.now();
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = IngestionStatus.FAILED;
        this.failureReason = truncate(reason);
    }

    /** Resets state so ingestion can run again; counts a retry only after a failure. */
    public void resetForIngestion() {
        if (this.status == IngestionStatus.FAILED) {
            this.retryCount++;
        }
        this.status = IngestionStatus.PENDING;
        this.failureReason = null;
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= MAX_FAILURE_REASON ? reason : reason.substring(0, MAX_FAILURE_REASON);
    }
}
