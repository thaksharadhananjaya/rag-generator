package com.rag.raggeneratorapplication.port.processing;

/**
 * One embeddable slice of a processed document.
 *
 * @param ordinal 0-based position within the document
 * @param text    the chunk text
 * @param page    source page number, or {@code null} when the format is unpaged
 */
public record ProcessedChunk(int ordinal, String text, Integer page) {
}
