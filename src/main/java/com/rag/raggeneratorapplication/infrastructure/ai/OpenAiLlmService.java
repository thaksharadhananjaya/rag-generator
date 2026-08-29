package com.rag.raggeneratorapplication.infrastructure.ai;

import com.rag.raggeneratorapplication.config.props.OpenAiProperties;
import com.rag.raggeneratorapplication.port.llm.LlmRequest;
import com.rag.raggeneratorapplication.port.llm.LlmResponse;
import com.rag.raggeneratorapplication.port.llm.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * LLM adapter for the OpenAI {@code /chat/completions} API.
 *
 * <p>Pass 1 stub: wired and injectable, behaviour lands with the rag module.
 * Responsible (Pass 2) for request/response mapping, timeouts, a small retry with
 * backoff, and translating failures to
 * {@link com.rag.raggeneratorapplication.exception.DependencyUnavailableException}.
 */
@Component
@RequiredArgsConstructor
public class OpenAiLlmService implements LlmService {

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;

    @Override
    public LlmResponse generate(LlmRequest request) {
        throw new UnsupportedOperationException("OpenAiLlmService.generate is implemented in Pass 2");
    }
}
