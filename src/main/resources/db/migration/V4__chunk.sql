-- Owned by the VectorStore port (PgVectorStore adapter). Chunk text, provenance
-- and embedding are persisted together so retrieval is a single query.
CREATE TABLE chunk (
    id                UUID PRIMARY KEY,
    -- Every retrieval is scoped by knowledge_base_id; it is duplicated here (not
    -- only reachable via document) so the scope filter never needs a join.
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_base (id) ON DELETE CASCADE,
    document_id       UUID NOT NULL REFERENCES document (id) ON DELETE CASCADE,
    ordinal           INT NOT NULL,
    content           TEXT NOT NULL,
    -- Provenance for citations; page is null when the source format is unpaged.
    page              INT,
    start_offset      INT,
    end_offset        INT,
    embedding_model   VARCHAR(100) NOT NULL,
    -- Fixed dimension: text-embedding-3-small = 1536. A different embedding model
    -- with a different dimension requires a new migration and a re-embed.
    embedding         vector(1536) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, ordinal)
);

CREATE INDEX ix_chunk_knowledge_base ON chunk (knowledge_base_id);
CREATE INDEX ix_chunk_document ON chunk (document_id);

-- HNSW index for approximate nearest-neighbour search under cosine distance.
-- Retrieval similarity is defined as 1 - (embedding <=> query_embedding).
CREATE INDEX ix_chunk_embedding_hnsw
    ON chunk USING hnsw (embedding vector_cosine_ops);
