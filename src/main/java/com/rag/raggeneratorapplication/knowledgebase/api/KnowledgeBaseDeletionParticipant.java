package com.rag.raggeneratorapplication.knowledgebase.api;

import java.util.UUID;

/**
 * Extension point for other modules to clean up knowledge-base-scoped state when
 * a knowledge base is deleted. This is the knowledgebase module's explicit
 * outward contract; the module never reaches into other modules directly.
 *
 * <p>Implementations are discovered as Spring beans and invoked by
 * {@code KnowledgeBaseService.delete} <em>before</em> the row is removed, so an
 * implementation can still query its own KB-scoped rows. It runs inside the
 * delete transaction; work against external systems (e.g. object storage) must be
 * idempotent, since a later failure may leave that work done without the row
 * being removed.
 */
public interface KnowledgeBaseDeletionParticipant {

    void beforeKnowledgeBaseDeleted(UUID knowledgeBaseId);
}
