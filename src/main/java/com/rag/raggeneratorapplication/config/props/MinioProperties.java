package com.rag.raggeneratorapplication.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connection settings for the MinIO-backed {@code FileStorageService} adapter. */
@ConfigurationProperties(prefix = "app.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket) {

    public MinioProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "http://localhost:9000";
        }
        if (accessKey == null || accessKey.isBlank()) {
            accessKey = "minioadmin";
        }
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = "minioadmin";
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "rag-documents";
        }
    }
}
