package com.rag.raggeneratorapplication.infrastructure.ai.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rag.raggeneratorapplication.config.props.OpenAiProperties;
import com.rag.raggeneratorapplication.exception.DependencyUnavailableException;
import com.rag.raggeneratorapplication.port.llm.LlmMessage;
import com.rag.raggeneratorapplication.port.llm.LlmRequest;
import com.rag.raggeneratorapplication.port.llm.LlmResponse;
import com.rag.raggeneratorapplication.port.llm.LlmService;
import com.rag.raggeneratorapplication.port.llm.TokenUsage;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * LLM adapter for the OpenAI {@code /chat/completions} API. Maps the neutral
 * {@link LlmRequest}/{@link LlmResponse} to the provider wire format, retries a
 * failed call a couple of times, and surfaces {@link DependencyUnavailableException}.
 *
 * <p>Active unless {@code app.ai.provider=ollama}.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai")
@RequiredArgsConstructor
@Slf4j
public class OpenAiLlmService implements LlmService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 500L;

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;

    @Override
    public LlmResponse generate(LlmRequest request) {
        String model = request.model() != null ? request.model() : properties.chatModel();
        ChatCompletionRequest body = new ChatCompletionRequest(
                model,
                request.messages().stream().map(OpenAiLlmService::toApiMessage).toList(),
                request.temperature(),
                request.maxTokens());

        ChatCompletionResponse response = callWithRetry(body);
        if (response.choices() == null || response.choices().isEmpty()) {
            throw new DependencyUnavailableException("LLM provider returned no choices", null);
        }
        Choice choice = response.choices().getFirst();
        TokenUsage usage = response.usage() == null ? TokenUsage.UNKNOWN : new TokenUsage(
                response.usage().promptTokens(), response.usage().completionTokens(), response.usage().totalTokens());
        String content = choice.message() == null ? "" : choice.message().content();
        return new LlmResponse(content, response.model() != null ? response.model() : model, usage, choice.finishReason());
    }

    private ChatCompletionResponse callWithRetry(ChatCompletionRequest body) {
        RestClientException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ChatCompletionResponse response = openAiRestClient.post()
                        .uri("/chat/completions")
                        .body(body)
                        .retrieve()
                        .body(ChatCompletionResponse.class);
                if (response == null) {
                    throw new DependencyUnavailableException("LLM provider returned an empty response", null);
                }
                return response;
            } catch (RestClientException e) {
                last = e;
                log.warn("LLM call attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.toString());
                sleepBeforeRetry(attempt);
            }
        }
        throw new DependencyUnavailableException("LLM provider call failed after " + MAX_ATTEMPTS + " attempts", last);
    }

    private static ChatMessage toApiMessage(LlmMessage message) {
        return new ChatMessage(message.role().name().toLowerCase(Locale.ROOT), message.content());
    }

    private static void sleepBeforeRetry(int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            return;
        }
        try {
            Thread.sleep(RETRY_BACKOFF_MS * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DependencyUnavailableException("LLM retry interrupted", ie);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatCompletionRequest(String model, List<ChatMessage> messages, Double temperature,
                                         @JsonProperty("max_tokens") Integer maxTokens) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatCompletionResponse(String model, List<Choice> choices, Usage usage) {
    }

    private record Choice(ChatMessage message, @JsonProperty("finish_reason") String finishReason) {
    }

    private record Usage(@JsonProperty("prompt_tokens") int promptTokens,
                         @JsonProperty("completion_tokens") int completionTokens,
                         @JsonProperty("total_tokens") int totalTokens) {
    }
}
