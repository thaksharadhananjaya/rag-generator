package com.rag.raggeneratorapplication.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Ollama-backed LLM and embedding adapters (local models).
 * Active only when {@code app.ai.provider=ollama}.
 *
 * @param baseUrl            Ollama server, e.g. {@code http://localhost:11434}
 * @param chatModel          model tag for {@code /api/chat}, e.g. {@code llama3.1}
 * @param embeddingModel     model tag for {@code /api/embed}, e.g. {@code nomic-embed-text}
 * @param embeddingDimension vector length {@code embeddingModel} produces; must match the
 *                           {@code vector(N)} column in the schema (see V4 migration)
 * @param timeoutSeconds     read timeout &mdash; local models can be slow, and the first
 *                           call after a cold start also pays model-load time
 */
@ConfigurationProperties(prefix = "app.ollama")
public record OllamaProperties(
        String baseUrl,
        String chatModel,
        String embeddingModel,
        int embeddingDimension,
        int timeoutSeconds) {

    public OllamaProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        if (chatModel == null || chatModel.isBlank()) {
            chatModel = "llama3.1";
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            embeddingModel = "nomic-embed-text";
        }
        if (embeddingDimension <= 0) {
            embeddingDimension = 768;
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 60;
        }
    }
}
