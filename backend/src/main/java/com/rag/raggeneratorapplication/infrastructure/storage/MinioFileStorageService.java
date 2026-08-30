package com.rag.raggeneratorapplication.infrastructure.storage;

import com.rag.raggeneratorapplication.config.props.MinioProperties;
import com.rag.raggeneratorapplication.exception.DependencyUnavailableException;
import com.rag.raggeneratorapplication.exception.NotFoundException;
import com.rag.raggeneratorapplication.port.storage.FileContent;
import com.rag.raggeneratorapplication.port.storage.FileStorageService;
import com.rag.raggeneratorapplication.port.storage.FileUpload;
import com.rag.raggeneratorapplication.port.storage.StoredFile;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link FileStorageService} backed by MinIO (Object Storage).
 * The bucket is created on first use. The object key is supplied by the caller.
 *
 * @author Thakshara Dhananjaya
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MinioClient minioClient;
    private final MinioProperties properties;

    /**
     * Stores an uploaded file in the configured MinIO bucket.
     *
     * @param storageKey object key used to identify the stored file
     * @param upload file content and metadata to store
     * @return metadata describing the stored file
     * @throws DependencyUnavailableException if MinIO is unavailable
     */
    @Override
    public StoredFile store(String storageKey, FileUpload upload) {
        ensureBucketExists();
        String contentType = upload.contentType() != null ? upload.contentType() : DEFAULT_CONTENT_TYPE;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(storageKey)
                    .stream(upload.content(), upload.size(), -1)
                    .contentType(contentType)
                    .build());
            log.info(
                    "[Minio] File stored successfully in MinIO: bucket={}, storageKey={}, size={}, contentType={}",
                    properties.bucket(),
                    storageKey,
                    upload.size(),
                    contentType
            );
        } catch (Exception e) {
            log.error(
                    "[Minio] Failed to store file in MinIO: bucket={}, storageKey={}, size={}",
                    properties.bucket(),
                    storageKey,
                    upload.size(),
                    e
            );
            throw new DependencyUnavailableException("Failed to store file in Minio object storage", e);
        }
        return new StoredFile(storageKey, upload.size(), contentType);
    }

    /**
     * Retrieves a stored file from MinIO.
     *
     * @param storageKey object key identifying the stored file
     * @return file content and its storage metadata
     * @throws NotFoundException if the requested file does not exist
     * @throws DependencyUnavailableException if MinIO cannot be accessed
     */
    @Override
    public FileContent retrieve(String storageKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket()).object(storageKey).build());
            GetObjectResponse object = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.bucket()).object(storageKey).build());
            return new FileContent(object, stat.contentType(), stat.size());
        } catch (ErrorResponseException e) {
            if (isNotFound(e)) {
                throw new NotFoundException("Stored file '" + storageKey + "' not found");
            }
            throw new DependencyUnavailableException("Failed to read file from object storage", e);
        } catch (Exception e) {
            throw new DependencyUnavailableException("Failed to read file from object storage", e);
        }
    }

    /**
     * Deletes a stored file from MinIO.
     *
     * <p>The operation is idempotent: deleting a file that does not exist
     * is treated as a successful operation.
     *
     * @param storageKey object key identifying the file to delete
     * @throws DependencyUnavailableException if MinIO cannot be accessed
     */
    @Override
    public void delete(String storageKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket()).object(storageKey).build());
        } catch (ErrorResponseException e) {
            if (isNotFound(e)) {
                return;
            }
            throw new DependencyUnavailableException("Failed to delete file from object storage", e);
        } catch (Exception e) {
            throw new DependencyUnavailableException("Failed to delete file from object storage", e);
        }
    }

    /**
     * Ensures that the configured MinIO bucket exists.
     *
     * <p>The bucket is created lazily when the first file is stored.
     *
     * @throws DependencyUnavailableException if the bucket cannot be checked
     *                                        or created
     */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.bucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
            }
        } catch (Exception e) {
            throw new DependencyUnavailableException("Object storage is unavailable", e);
        }
    }

    /**
     * Determines whether a MinIO error indicates that an object does not exist.
     *
     * @param e MinIO error response
     * @return {@code true} when the error represents a missing object
     */
    private static boolean isNotFound(ErrorResponseException e) {
        String code = e.errorResponse() == null ? null : e.errorResponse().code();
        return "NoSuchKey".equals(code) || "NoSuchObject".equals(code);
    }
}
