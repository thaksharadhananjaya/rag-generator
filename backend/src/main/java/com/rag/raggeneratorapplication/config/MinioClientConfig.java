package com.rag.raggeneratorapplication.config;

import com.rag.raggeneratorapplication.config.props.MinioProperties;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the MinIO client. The builder only assembles configuration; no network
 * connection is made until the {@code FileStorageService} adapter is used.
 */
@Configuration(proxyBeanMethods = false)
public class MinioClientConfig {

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }
}
