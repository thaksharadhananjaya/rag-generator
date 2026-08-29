package com.rag.raggeneratorapplication.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata for the generated OpenAPI document / Swagger UI (served at /docs). */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI ragOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("RAG Generator API")
                .version("v1")
                .description("Knowledge bases, asynchronous document ingestion, "
                        + "and grounded retrieval-augmented generation with sources."));
    }
}
