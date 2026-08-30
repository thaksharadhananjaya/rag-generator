package com.rag.raggeneratorapplication.infrastructure.vector;

import com.rag.raggeneratorapplication.port.vector.VectorChunk;
import com.rag.raggeneratorapplication.port.vector.VectorMatch;
import com.rag.raggeneratorapplication.port.vector.VectorSearchQuery;
import com.rag.raggeneratorapplication.port.vector.VectorStore;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link VectorStore} backed by PostgreSQL + pgvector against the {@code chunk} table.
 * Chunk text, provenance and embedding are stored together so retrieval is one query.
 */
@Component
@RequiredArgsConstructor
public class PgVectorStore implements VectorStore {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    @Transactional
    public void replaceDocumentChunks(UUID knowledgeBaseId, UUID documentId, List<VectorChunk> chunks) {
        jdbc.update(
                "DELETE FROM chunk WHERE knowledge_base_id = :kb AND document_id = :doc",
                new MapSqlParameterSource().addValue("kb", knowledgeBaseId).addValue("doc", documentId));

        if (chunks.isEmpty()) {
            return;
        }

        MapSqlParameterSource[] batch = chunks.stream()
                .map(chunk -> new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("kb", knowledgeBaseId)
                        .addValue("doc", documentId)
                        .addValue("ordinal", chunk.ordinal())
                        .addValue("content", chunk.text())
                        .addValue("page", chunk.page())
                        .addValue("startOffset", chunk.startOffset())
                        .addValue("endOffset", chunk.endOffset())
                        .addValue("model", chunk.embeddingModel())
                        .addValue("embedding", toVectorLiteral(chunk.embedding())))
                .toArray(MapSqlParameterSource[]::new);

        jdbc.batchUpdate("""
                INSERT INTO chunk (id, knowledge_base_id, document_id, ordinal, content,
                                   page, start_offset, end_offset, embedding_model, embedding)
                VALUES (:id, :kb, :doc, :ordinal, :content,
                        :page, :startOffset, :endOffset, :model, CAST(:embedding AS vector))
                """, batch);
    }

    @Override
    public void deleteByDocument(UUID knowledgeBaseId, UUID documentId) {
        jdbc.update(
                "DELETE FROM chunk WHERE knowledge_base_id = :kb AND document_id = :doc",
                new MapSqlParameterSource().addValue("kb", knowledgeBaseId).addValue("doc", documentId));
    }

    @Override
    public void deleteByKnowledgeBase(UUID knowledgeBaseId) {
        jdbc.update(
                "DELETE FROM chunk WHERE knowledge_base_id = :kb",
                new MapSqlParameterSource("kb", knowledgeBaseId));
    }

    @Override
    public List<VectorMatch> search(VectorSearchQuery query) {
        // Cosine distance via the HNSW index; similarity = 1 - distance. The
        // threshold repeats the expression because the alias is not visible in WHERE.
        String sql = """
                SELECT id, document_id, ordinal, content, page,
                       1 - (embedding <=> CAST(:q AS vector)) AS score
                FROM chunk
                WHERE knowledge_base_id = :kb
                  AND embedding_model = :model
                  AND 1 - (embedding <=> CAST(:q AS vector)) >= :minScore
                ORDER BY embedding <=> CAST(:q AS vector)
                LIMIT :topK
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("kb", query.knowledgeBaseId())
                .addValue("model", query.embeddingModel())
                .addValue("q", toVectorLiteral(query.queryEmbedding()))
                .addValue("minScore", query.minScore())
                .addValue("topK", query.topK());
        return jdbc.query(sql, params, VECTOR_MATCH_MAPPER);
    }

    private static final RowMapper<VectorMatch> VECTOR_MATCH_MAPPER = (rs, rowNum) -> new VectorMatch(
            rs.getObject("id", UUID.class),
            rs.getObject("document_id", UUID.class),
            rs.getInt("ordinal"),
            rs.getString("content"),
            rs.getObject("page") == null ? null : rs.getInt("page"),
            rs.getDouble("score"));

    /** pgvector text form: {@code [f1,f2,...]}. */
    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8 + 2).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
