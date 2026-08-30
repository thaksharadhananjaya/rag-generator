package com.rag.raggeneratorapplication.infrastructure.ai.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rag.raggeneratorapplication.config.props.OpenAiProperties;
import com.rag.raggeneratorapplication.exception.DependencyUnavailableException;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingResult;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Embedding adapter for the OpenAI {@code /embeddings} API. Splits large inputs
 * into fixed-size batches and retries a failed batch a couple of times before
 * surfacing {@link DependencyUnavailableException}.
 *
 * <p>Active unless {@code app.ai.provider=ollama}.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai")
@RequiredArgsConstructor
@Slf4j
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final int MAX_BATCH = 96;
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 500L;

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;

    @Override
    public EmbeddingResult embed(List<String> texts) {
        log.info(
                "[Open AI] Starting embedding generation: textCount={}, model={}",
                texts.size(),
                modelId()
        );
        if (texts.isEmpty()) {
            log.info("[Open AI] Embedding request contains no texts");
            return new EmbeddingResult(List.of(), modelId(), dimension(), 0);
        }
        List<float[]> vectors = new ArrayList<>(texts.size());
        int totalTokens = 0;
        for (int from = 0; from < texts.size(); from += MAX_BATCH) {
            List<String> batch = texts.subList(from, Math.min(from + MAX_BATCH, texts.size()));
            EmbeddingApiResponse response = callWithRetry(batch);
            response.data().stream()
                    .sorted(Comparator.comparingInt(EmbeddingData::index))
                    .forEach(d -> vectors.add(d.embedding()));
            if (response.usage() != null) {
                totalTokens += response.usage().totalTokens();
            }
        }
        log.info(
                "[Open AI] Embedding generation completed: textCount={}, vectorCount={}, model={}, dimension={}, totalTokens={}",
                texts.size(),
                vectors.size(),
                modelId(),
                dimension(),
                totalTokens
        );
        return new EmbeddingResult(vectors, modelId(), dimension(), totalTokens);
    }

    @Override
    public String modelId() {
        return properties.embeddingModel();
    }

    @Override
    public int dimension() {
        return properties.embeddingDimension();
    }

    private EmbeddingApiResponse callWithRetry(List<String> batch) {
        RestClientException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call(batch);
            } catch (RestClientException e) {
                last = e;
                log.warn("[Open AI] Embedding call attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.toString());
                sleepBeforeRetry(attempt);
            }
        }
        throw new DependencyUnavailableException("Embedding provider call failed after " + MAX_ATTEMPTS + " attempts", last);
    }

    private EmbeddingApiResponse call(List<String> batch) {
        EmbeddingApiResponse response = openAiRestClient.post()
                .uri("/embeddings")
                .body(new EmbeddingApiRequest(properties.embeddingModel(), batch))
                .retrieve()
                .body(EmbeddingApiResponse.class);
        if (response == null || response.data() == null || response.data().size() != batch.size()) {
            log.error(
                    "[Open AI] Returned unexpected number of vectors: expected={}, actual={}, model={}",
                    batch.size(),
                    response.data() == null ? 0 : response.data().size(),
                    modelId()
            );

            throw new DependencyUnavailableException("Embedding provider returned an unexpected response", null);
        }
        return response;
    }

    private static void sleepBeforeRetry(int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            return;
        }
        try {
            Thread.sleep(RETRY_BACKOFF_MS * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DependencyUnavailableException("Embedding retry interrupted", ie);
        }
    }

    private record EmbeddingApiRequest(String model, List<String> input) {
    }

    private record EmbeddingApiResponse(List<EmbeddingData> data, String model, Usage usage) {
    }

    private record EmbeddingData(int index, float[] embedding) {
    }

    private record Usage(@JsonProperty("prompt_tokens") int promptTokens,
                         @JsonProperty("total_tokens") int totalTokens) {
    }
}
