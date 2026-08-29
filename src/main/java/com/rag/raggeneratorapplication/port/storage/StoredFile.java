package com.rag.raggeneratorapplication.port.storage;

/**
 * Reference to a persisted file. {@code storageKey} is opaque to callers and is
 * the only handle needed to retrieve or delete the object later; it is what the
 * document module persists.
 */
public record StoredFile(String storageKey, long size, String contentType) {
}
