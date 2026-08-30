package com.rag.raggeneratorapplication.port.llm;

/**
 * Application-owned abstraction over a large language model.
 *
 * <p>Implementations live in {@code infrastructure} and are responsible for
 * provider wire format, authentication, timeouts and retries. Callers depend
 * only on this interface and the neutral request/response records.
 */
public interface LlmService {

    /**
     * Generates a completion for the given request.
     *
     * @throws com.rag.raggeneratorapplication.exception.DependencyUnavailableException
     *         if the provider fails or is unreachable after the adapter's retries
     */
    LlmResponse generate(LlmRequest request);
}
