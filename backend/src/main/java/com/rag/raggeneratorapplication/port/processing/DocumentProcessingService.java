package com.rag.raggeneratorapplication.port.processing;

import java.io.InputStream;
import java.util.List;

/**
 * Application-owned abstraction that turns an uploaded document into embeddable
 * chunks in a single step &mdash; text extraction and chunking together.
 *
 * <p>This replaces the separate {@code DocumentTextExtractor} and
 * {@code ChunkingService}. The implementation (Spring AI document readers plus a
 * token splitter) lives in {@code infrastructure}; callers depend only on this
 * interface.
 */
public interface DocumentProcessingService {

    /**
     * Extracts and chunks a document. The caller owns and closes {@code content}.
     *
     * @param filename    original filename, used to pick the reader and for hints
     * @throws com.rag.raggeneratorapplication.exception.ApiException if the content cannot be read
     */
    List<ProcessedChunk> process(String filename, InputStream content);

    /**
     * Identifier of the current reader/splitter configuration, persisted on the
     * document so a later change can be detected.
     */
    String configId();
}
