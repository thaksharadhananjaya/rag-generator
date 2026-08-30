-- Owned by the `document` module.
CREATE TABLE document (
    id                UUID PRIMARY KEY,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_base (id) ON DELETE CASCADE,
    filename          VARCHAR(512) NOT NULL,
    content_type      VARCHAR(255),
    size_bytes        BIGINT NOT NULL,
    -- Opaque key returned by FileStorageService; the original bytes live in
    -- object storage (MinIO), never in PostgreSQL.
    storage_key       VARCHAR(512) NOT NULL,
    -- Ingestion state: PENDING -> PROCESSING -> EMBEDDING
    -- -> COMPLETED | FAILED. Advanced by the async ingestion worker.
    status            VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    failure_reason    TEXT,
    retry_count       INT NOT NULL DEFAULT 0,
    chunk_count       INT NOT NULL DEFAULT 0,
    -- Records the chunking parameters that produced the current chunks, so a
    -- later change of strategy can be detected and the document re-ingested.
    chunking_config   VARCHAR(255),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    ingested_at       TIMESTAMPTZ
);

CREATE INDEX ix_document_knowledge_base ON document (knowledge_base_id);
CREATE INDEX ix_document_status ON document (status);
