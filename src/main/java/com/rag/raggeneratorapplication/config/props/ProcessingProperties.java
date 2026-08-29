package com.rag.raggeneratorapplication.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning for the Spring AI document processing pipeline (token splitter).
 *
 * @param chunkSizeTokens   target chunk size in tokens
 * @param minChunkSizeChars minimum characters before a chunk may be split at a
 *                          sentence boundary
 */
@ConfigurationProperties(prefix = "app.processing")
public record ProcessingProperties(int chunkSizeTokens, int minChunkSizeChars) {

    public ProcessingProperties {
        if (chunkSizeTokens <= 0) {
            chunkSizeTokens = 800;
        }
        if (minChunkSizeChars <= 0) {
            minChunkSizeChars = 350;
        }
    }
}
