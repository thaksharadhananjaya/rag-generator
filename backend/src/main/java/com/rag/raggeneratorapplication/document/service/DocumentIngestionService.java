package com.rag.raggeneratorapplication.document.service;

import com.rag.raggeneratorapplication.config.AsyncConfig;
import com.rag.raggeneratorapplication.document.entity.Document;
import com.rag.raggeneratorapplication.document.repository.DocumentRepository;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingResult;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingService;
import com.rag.raggeneratorapplication.port.processing.DocumentProcessingService;
import com.rag.raggeneratorapplication.port.processing.ProcessedChunk;
import com.rag.raggeneratorapplication.port.storage.FileContent;
import com.rag.raggeneratorapplication.port.storage.FileStorageService;
import com.rag.raggeneratorapplication.port.vector.VectorChunk;
import com.rag.raggeneratorapplication.port.vector.VectorStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orchestrates the ingestion pipeline:
 * {@code storage -> process (extract + chunk) -> embed -> vector store}. Each
 * boundary is an application port, so any stage can be swapped without touching
 * this class.
 *
 * <p>Runs on the {@link AsyncConfig#INGESTION_EXECUTOR} pool. Status transitions
 * are written in their own short transactions (via {@link TransactionTemplate})
 * so {@code GET /documents/{id}} reflects progress while a document is still
 * being processed. The heavy, external steps run outside any transaction.
 *
 * <p>Keeps an explicit constructor because it derives a {@link TransactionTemplate}
 * from the injected transaction manager rather than storing a plain dependency.
 */
@Service
@Slf4j
public class DocumentIngestionService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorage;
    private final DocumentProcessingService documentProcessingService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final TransactionTemplate transactionTemplate;

    public DocumentIngestionService(DocumentRepository documentRepository,
                                    FileStorageService fileStorage,
                                    DocumentProcessingService documentProcessingService,
                                    EmbeddingService embeddingService,
                                    VectorStore vectorStore,
                                    PlatformTransactionManager transactionManager) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
        this.documentProcessingService = documentProcessingService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Async(AsyncConfig.INGESTION_EXECUTOR)
    public void ingestAsync(UUID documentId) {
        try {
            runPipeline(documentId);
        } catch (Exception e) {
            log.error("Ingestion failed for document {}: {}", documentId, e.toString(), e);
            safeMarkFailed(documentId, e);
        }
    }

    private void runPipeline(UUID documentId) throws Exception {
        Document document = transactionTemplate.execute(status ->
                documentRepository.findById(documentId).orElse(null));
        if (document == null) {
            log.warn("Document {} vanished before ingestion started", documentId);
            return;
        }
        UUID knowledgeBaseId = document.getKnowledgeBaseId();

        // 1. Extract text and chunk it (Spring AI), as a single step.
        inTx(documentId, Document::markProcessing);
        List<ProcessedChunk> chunks;
        try (FileContent file = fileStorage.retrieve(document.getStorageKey())) {
            chunks = documentProcessingService.process(
                    document.getFilename(), file.content());
        }
        if (chunks.isEmpty()) {
            inTx(documentId, d -> d.markFailed("No chunks produced from the document"));
            return;
        }

        // 2. Embed.
        inTx(documentId, Document::markEmbedding);
        EmbeddingResult embeddings = embeddingService.embed(chunks.stream().map(ProcessedChunk::text).toList());
        if (embeddings.vectors().size() != chunks.size()) {
            throw new IllegalStateException("Embedding count " + embeddings.vectors().size()
                    + " does not match chunk count " + chunks.size());
        }

        // 3. Persist vectors (replace = idempotent re-ingest).
        vectorStore.replaceDocumentChunks(knowledgeBaseId, documentId, toVectorChunks(chunks, embeddings));

        // 4. Done.
        String configId = documentProcessingService.configId();
        int chunkCount = chunks.size();
        inTx(documentId, d -> d.markCompleted(chunkCount, configId));
    }

    private static List<VectorChunk> toVectorChunks(List<ProcessedChunk> chunks, EmbeddingResult embeddings) {
        List<VectorChunk> vectorChunks = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            ProcessedChunk c = chunks.get(i);
            vectorChunks.add(new VectorChunk(
                    c.ordinal(), c.text(), embeddings.vectors().get(i), embeddings.model(),
                    c.page(), null, null));
        }
        return vectorChunks;
    }

    private void inTx(UUID documentId, Consumer<Document> mutation) {
        transactionTemplate.executeWithoutResult(status -> {
            Document document = documentRepository.findById(documentId).orElseThrow(() ->
                    new IllegalStateException("Document " + documentId + " no longer exists"));
            mutation.accept(document);
        });
    }

    private void safeMarkFailed(UUID documentId, Throwable cause) {
        try {
            inTx(documentId, d -> d.markFailed(describe(cause)));
        } catch (RuntimeException e) {
            log.error("Could not record FAILED status for document {}", documentId, e);
        }
    }

    private static String describe(Throwable cause) {
        String message = cause.getMessage();
        return cause.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
