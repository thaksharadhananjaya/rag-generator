package com.rag.raggeneratorapplication.port.llm;

import java.util.List;

/**
 * A chat-completion request expressed in provider-neutral terms.
 *
 * @param messages    ordered conversation, usually a system prompt followed by a user turn
 * @param model       provider model id, or {@code null} to use the adapter's configured default
 * @param temperature sampling temperature, or {@code null} for the provider default
 * @param maxTokens   response token cap, or {@code null} for the provider default
 */
public record LlmRequest(
        List<LlmMessage> messages,
        String model,
        Double temperature,
        Integer maxTokens) {

    public LlmRequest {
        messages = List.copyOf(messages);
    }

    public static LlmRequest of(List<LlmMessage> messages) {
        return new LlmRequest(messages, null, null, null);
    }
}
