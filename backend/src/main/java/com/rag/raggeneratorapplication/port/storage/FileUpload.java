package com.rag.raggeneratorapplication.port.storage;

import java.io.InputStream;

/**
 * Bytes and metadata handed to {@link FileStorageService#store(FileUpload)}.
 * The caller owns {@code content} and closes it after the call returns.
 *
 * @param filename    original filename, used only to derive a readable storage key
 * @param contentType MIME type, or {@code null} if unknown
 * @param size        length of {@code content} in bytes
 * @param content     the file bytes; read once, not buffered by this record
 */
public record FileUpload(
        String filename,
        String contentType,
        long size,
        InputStream content) {
}
