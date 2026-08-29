package com.rag.raggeneratorapplication.document.entity;

/**
 * Lifecycle of a document's ingestion. Advances
 * {@code PENDING -> PROCESSING -> EMBEDDING -> COMPLETED}, or to {@code FAILED}
 * from any step. {@code PROCESSING} covers Spring AI text extraction and chunking
 * as a single step.
 */
public enum IngestionStatus {

    PENDING,
    PROCESSING,
    EMBEDDING,
    COMPLETED,
    FAILED;

    /** Whether ingestion may be (re)started from this state (i.e. it is not mid-flight). */
    public boolean canTrigger() {
        return this == PENDING || this == FAILED || this == COMPLETED;
    }
}
