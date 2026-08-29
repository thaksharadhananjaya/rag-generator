package com.rag.raggeneratorapplication.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the OpenAI-backed LLM and embedding adapters.
 * {@code apiKey} may be blank at startup; adapters fail with a clear error only
 * when actually invoked without it.
 *
 * @param embeddingDimension vector length produced by {@code embeddingModel};
 *                           must match the {@code vector(N)} column in the schema
 */
@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
        String baseUrl,
        String apiKey,
        String chatModel,
        String embeddingModel,
        int embeddingDimension,
        int timeoutSeconds) {

    public OpenAiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        if (chatModel == null || chatModel.isBlank()) {
            chatModel = "gpt-4o-mini";
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            embeddingModel = "text-embedding-3-small";
        }
        if (embeddingDimension <= 0) {
            embeddingDimension = 1536;
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 60;
        }
    }
}
