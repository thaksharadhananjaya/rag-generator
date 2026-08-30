package com.rag.raggeneratorapplication.config.props;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the OpenAI-backed LLM and embedding adapters.
 * {@code apiKey} may be blank at startup; adapters fail with a clear error only
 * when actually invoked without it.
 *
 * @param embeddingDimension vector length produced by {@code embeddingModel};
 *                           must match the {@code vector(N)} column in the schema
 */
@ConfigurationProperties(prefix = "app.openai")
@Validated
public record OpenAiProperties(

        @NotBlank
        String baseUrl,

        @NotBlank
        String apiKey,

        @NotBlank
        String chatModel,

        @NotBlank
        String embeddingModel,

        @Min(1)
        int embeddingDimension,

        @Min(1)
        int timeoutSeconds) {
}