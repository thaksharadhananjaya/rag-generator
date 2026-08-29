package com.rag.raggeneratorapplication.config.props;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Connection settings for the MinIO-backed {@code FileStorageService} adapter. */
@Validated
@ConfigurationProperties(prefix = "app.minio")
public record MinioProperties(
        @NotBlank
        String endpoint,

        @NotBlank
        String accessKey,

        @NotBlank
        String secretKey,

        @NotBlank
        String bucket) {
}
