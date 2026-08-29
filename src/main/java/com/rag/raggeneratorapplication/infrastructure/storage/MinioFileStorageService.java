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
 * {@link FileStorageService} backed by MinIO (S3-compatible). The bucket is
 * created on first use. The object key is supplied by the caller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MinioClient minioClient;
    private final MinioProperties properties;

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

    private static boolean isNotFound(ErrorResponseException e) {
        String code = e.errorResponse() == null ? null : e.errorResponse().code();
        return "NoSuchKey".equals(code) || "NoSuchObject".equals(code);
    }
}
