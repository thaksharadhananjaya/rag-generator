package com.rag.raggeneratorapplication.infrastructure.ai.ollama;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rag.raggeneratorapplication.config.props.OllamaProperties;
import com.rag.raggeneratorapplication.exception.DependencyUnavailableException;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingResult;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Embedding adapter for a local Ollama server ({@code POST /api/embed}).
 * Active when {@code app.ai.provider=ollama}.
 *
 * <p>Ollama embedding models produce their own dimensions (e.g.
 * {@code nomic-embed-text} = 768), which must match the {@code chunk.embedding}
 * column; {@link #dimension()} comes from {@code app.ollama.embedding-dimension}.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OllamaEmbeddingService implements EmbeddingService {
    private final RestClient ollamaRestClient;
    private final OllamaProperties properties;

    @Override
    public EmbeddingResult embed(List<String> texts) {
        if (texts.isEmpty()) {
            return new EmbeddingResult(List.of(), modelId(), dimension(), 0);
        }
        EmbedResponse response;
        try {
            response = ollamaRestClient.post()
                    .uri("/api/embed")
                    .body(new EmbedRequest(properties.embeddingModel(), texts))
                    .retrieve()
                    .body(EmbedResponse.class);
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Ollama embeddings call to " + properties.baseUrl() + " failed", e);
        }
        if (response == null || response.embeddings() == null || response.embeddings().size() != texts.size()) {
            throw new DependencyUnavailableException("Ollama returned an unexpected embeddings response", null);
        }
        Integer tokens = response.promptEvalCount();
        return new EmbeddingResult(List.copyOf(response.embeddings()), modelId(), dimension(),
                tokens == null ? 0 : tokens);
    }

    @Override
    public String modelId() {
        return properties.embeddingModel();
    }

    @Override
    public int dimension() {
        return properties.embeddingDimension();
    }

    private record EmbedRequest(String model, List<String> input) {
    }

    private record EmbedResponse(List<float[]> embeddings,
                                 @JsonProperty("prompt_eval_count") Integer promptEvalCount) {
    }
}
