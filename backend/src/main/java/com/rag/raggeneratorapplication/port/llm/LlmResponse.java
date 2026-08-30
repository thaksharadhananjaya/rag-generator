package com.rag.raggeneratorapplication.port.llm;

/**
 * Result of a chat-completion call.
 *
 * @param content      the generated assistant text
 * @param model        the provider model that produced it
 * @param usage        token accounting, or {@link TokenUsage#UNKNOWN} if not reported
 * @param finishReason provider-reported stop reason (e.g. {@code stop}, {@code length}), may be {@code null}
 */
public record LlmResponse(
        String content,
        String model,
        TokenUsage usage,
        String finishReason) {
}
