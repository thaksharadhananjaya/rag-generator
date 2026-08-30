package com.rag.raggeneratorapplication.infrastructure.ai.ollama;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rag.raggeneratorapplication.config.props.OllamaProperties;
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
 * LLM adapter for a local Ollama server ({@code POST /api/chat}, non-streaming).
 * Active when {@code app.ai.provider=ollama}. Maps the neutral
 * {@link LlmRequest}/{@link LlmResponse} to Ollama's wire format.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "ollama", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OllamaLlmService implements LlmService {

    private final RestClient ollamaRestClient;
    private final OllamaProperties properties;

    @Override
    public LlmResponse generate(LlmRequest request) {
        String model = request.model() != null ? request.model() : properties.chatModel();
        Options options = (request.temperature() == null && request.maxTokens() == null)
                ? null
                : new Options(request.temperature(), request.maxTokens());
        ChatRequest body = new ChatRequest(
                model,
                request.messages().stream().map(OllamaLlmService::toApiMessage).toList(),
                false,
                options);

        ChatResponse response;
        try {
            response = ollamaRestClient.post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Ollama chat call to " + properties.baseUrl() + " failed", e);
        }
        if (response == null || response.message() == null) {
            throw new DependencyUnavailableException("Ollama returned an empty chat response", null);
        }

        int promptTokens = response.promptEvalCount() == null ? 0 : response.promptEvalCount();
        int completionTokens = response.evalCount() == null ? 0 : response.evalCount();
        TokenUsage usage = (promptTokens == 0 && completionTokens == 0)
                ? TokenUsage.UNKNOWN
                : new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens);

        return new LlmResponse(
                response.message().content(),
                response.model() != null ? response.model() : model,
                usage,
                response.doneReason());
    }

    private static Message toApiMessage(LlmMessage message) {
        return new Message(message.role().name().toLowerCase(Locale.ROOT), message.content());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatRequest(String model, List<Message> messages, boolean stream, Options options) {
    }

    private record Message(String role, String content) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Options(Double temperature, @JsonProperty("num_predict") Integer numPredict) {
    }

    private record ChatResponse(String model,
                                Message message,
                                @JsonProperty("done_reason") String doneReason,
                                @JsonProperty("prompt_eval_count") Integer promptEvalCount,
                                @JsonProperty("eval_count") Integer evalCount) {
    }
}
