package com.rag.raggeneratorapplication.port.storage;

/**
 * Application-owned abstraction over blob/object storage for original uploaded
 * documents. PostgreSQL keeps only the {@link StoredFile#storageKey()}.
 *
 * <p>Implementations live in {@code infrastructure} (MinIO initially; S3, GCS or
 * local disk are drop-in replacements).
 */
public interface FileStorageService {

    /**
     * Stores the given bytes under the caller-supplied key and returns a reference
     * to them. The key is chosen by the caller (e.g.
     * {@code knowledge-bases/{kbId}/documents/{docId}/file}) so storage layout
     * mirrors the domain.
     *
     * @throws com.rag.raggeneratorapplication.exception.DependencyUnavailableException
     *         if storage fails or is unreachable
     */
    StoredFile store(String storageKey, FileUpload upload);

    /**
     * Opens the stored object for reading.
     *
     * @throws com.rag.raggeneratorapplication.exception.NotFoundException if the key is unknown
     */
    FileContent retrieve(String storageKey);

    /** Deletes the stored object. A no-op if the key is already absent. */
    void delete(String storageKey);
}
