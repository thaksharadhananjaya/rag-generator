package com.rag.raggeneratorapplication.document;

import com.rag.raggeneratorapplication.document.entity.Document;
import com.rag.raggeneratorapplication.document.repository.DocumentRepository;
import com.rag.raggeneratorapplication.knowledgebase.api.KnowledgeBaseDeletionParticipant;
import com.rag.raggeneratorapplication.port.storage.FileStorageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Removes stored original files for a knowledge base's documents when that
 * knowledge base is deleted. The document and chunk <em>rows</em> are removed by
 * the {@code ON DELETE CASCADE} on the knowledge base row; only the external
 * object-storage objects need explicit cleanup here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class DocumentKnowledgeBaseCleanup implements KnowledgeBaseDeletionParticipant {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorage;

    @Override
    public void beforeKnowledgeBaseDeleted(UUID knowledgeBaseId) {
        for (Document document : documentRepository.findByKnowledgeBaseId(knowledgeBaseId)) {
            try {
                fileStorage.delete(document.getStorageKey());
            } catch (RuntimeException e) {
                // Best-effort: a missing or unreachable object must not block the deletion.
                log.warn("Could not delete stored file {} while deleting knowledge base {}",
                        document.getStorageKey(), knowledgeBaseId, e);
            }
        }
    }
}
