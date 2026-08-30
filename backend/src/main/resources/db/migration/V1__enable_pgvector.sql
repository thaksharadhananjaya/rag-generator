-- pgvector provides the `vector` column type and HNSW/IVFFlat ANN indexes.
-- Requires the pgvector extension to be available on the PostgreSQL server
-- (e.g. the `pgvector/pgvector` Docker image or `apt install postgresql-16-pgvector`).
CREATE EXTENSION IF NOT EXISTS vector;
