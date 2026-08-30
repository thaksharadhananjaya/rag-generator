package com.rag.raggeneratorapplication.knowledgebase.service;

import com.rag.raggeneratorapplication.exception.ConflictException;
import com.rag.raggeneratorapplication.exception.NotFoundException;
import com.rag.raggeneratorapplication.knowledgebase.api.KnowledgeBaseDeletionParticipant;
import com.rag.raggeneratorapplication.knowledgebase.dto.CreateKnowledgeBaseRequest;
import com.rag.raggeneratorapplication.knowledgebase.entity.KnowledgeBase;
import com.rag.raggeneratorapplication.knowledgebase.repository.KnowledgeBaseRepository;
import com.rag.raggeneratorapplication.port.vector.VectorStore;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Knowledge base lifecycle: create, retrieve, list, delete. Concrete by design
 *
 * @author Thakshara Dhananjaya
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository repository;
    private final VectorStore vectorStore;
    private final List<KnowledgeBaseDeletionParticipant> deletionParticipants;

    @Transactional
    public KnowledgeBase create(CreateKnowledgeBaseRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A knowledge base named '" + name + "' already exists");
        }
        //
        KnowledgeBase knowledgeBase = KnowledgeBase.builder().name(name).description(normalizeDescription(request.description())).build();
        KnowledgeBase savedKnowledgeBase = repository.save(knowledgeBase);
        log.info(
                "Knowledge base created successfully: id={}, name={}",
                knowledgeBase.getId(),
                knowledgeBase.getName()
        );
        //
        return savedKnowledgeBase;
    }

    public KnowledgeBase get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Knowledge base", id));
    }

    public Page<KnowledgeBase> getAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional
    public void delete(UUID id) {
        KnowledgeBase knowledgeBase = get(id);
        for (KnowledgeBaseDeletionParticipant participant : deletionParticipants) {
            participant.beforeKnowledgeBaseDeleted(id);
        }
        vectorStore.deleteByKnowledgeBase(id);
        repository.delete(knowledgeBase);
        log.info("Knowledge base deleted successfully: id={}", id);
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
