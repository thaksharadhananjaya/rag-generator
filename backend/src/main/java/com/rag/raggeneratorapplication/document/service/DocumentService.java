package com.rag.raggeneratorapplication.document.service;

import com.rag.raggeneratorapplication.config.props.DocumentProperties;
import com.rag.raggeneratorapplication.document.entity.Document;
import com.rag.raggeneratorapplication.document.repository.DocumentRepository;
import com.rag.raggeneratorapplication.exception.ConflictException;
import com.rag.raggeneratorapplication.exception.NotFoundException;
import com.rag.raggeneratorapplication.exception.UnsupportedMediaTypeException;
import com.rag.raggeneratorapplication.exception.ValidationException;
import com.rag.raggeneratorapplication.knowledgebase.service.KnowledgeBaseService;
import com.rag.raggeneratorapplication.port.storage.FileStorageService;
import com.rag.raggeneratorapplication.port.storage.FileUpload;
import com.rag.raggeneratorapplication.port.vector.VectorStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document lifecycle: upload &amp; validate, retrieve, list, delete, and
 * (re)trigger ingestion. Ingestion itself is delegated to
 * {@link DocumentIngestionService} and always started <em>after</em> the current
 *
 *
 * @author Thakshara Dhananjaya
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final FileStorageService fileStorage;
    private final VectorStore vectorStore;
    private final DocumentIngestionService ingestionService;
    private final DocumentProperties properties;

    @Transactional
    public Document upload(UUID knowledgeBaseId, MultipartFile file) {
        knowledgeBaseService.get(knowledgeBaseId);
        validate(file);

        UUID documentId = UUID.randomUUID();
        // upload file to object storage
        String storageKey = "knowledge-bases/%s/documents/%s_%s.pdf".formatted(knowledgeBaseId, documentId, file.getName());
        fileStorage.store(storageKey, toFileUpload(file));
        // create document record
        Document savedDocument = documentRepository.save(new Document(
                documentId,
                knowledgeBaseId,
                originalFilename(file),
                file.getContentType(),
                file.getSize(),
                storageKey));
        log.info(
                "Document record created successfully: id={}, file name={}",
                savedDocument.getId(),
                savedDocument.getFilename()
        );
        // build embeddings
        triggerAfterCommit(savedDocument.getId());
        return savedDocument;
    }

    public Document get(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> NotFoundException.of("Document", documentId));
    }

    public Page<Document> getAll(UUID knowledgeBaseId, Pageable pageable) {
        knowledgeBaseService.get(knowledgeBaseId);
        return documentRepository.findByKnowledgeBaseId(knowledgeBaseId, pageable);
    }

    @Transactional
    public Document reingest(UUID documentId) {
        Document document = get(documentId);
        if (!document.getStatus().canTrigger()) {
            throw new ConflictException(
                    "Document " + documentId + " is currently being ingested (" + document.getStatus() + ")");
        }
        document.resetForIngestion();
        triggerAfterCommit(documentId);
        return document;
    }

    @Transactional
    public void delete(UUID documentId) {
        Document document = get(documentId);
        vectorStore.deleteByDocument(document.getKnowledgeBaseId(), documentId);
        fileStorage.delete(document.getStorageKey());
        documentRepository.delete(document);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Uploaded file is empty");
        }
        if (originalFilename(file).isBlank()) {
            throw new ValidationException("Uploaded file has no name");
        }
        if (!properties.accepts(file.getContentType())) {
            throw new UnsupportedMediaTypeException(
                    "Content type '" + file.getContentType() + "' is not accepted for ingestion");
        }
    }

    private void triggerAfterCommit(UUID documentId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ingestionService.ingestAsync(documentId);
                }
            });
        } else {
            ingestionService.ingestAsync(documentId);
        }
    }

    private static FileUpload toFileUpload(MultipartFile file) {
        try {
            return new FileUpload(originalFilename(file), file.getContentType(), file.getSize(), file.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded file", e);
        }
    }

    private static String originalFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null ? "" : name;
    }
}
