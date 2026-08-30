package com.rag.raggeneratorapplication.port.llm;

/**
 * Represents token usage reported by an LLM provider.
 *
 * <p>This value is used for token accounting, logging, and usage budgeting.
 * The {@link #UNKNOWN} value is used when the provider does not report
 * token usage information.
 *
 * @author Thakshara Dhananjaya
 */

public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {

    public static final TokenUsage UNKNOWN = new TokenUsage(0, 0, 0);
}
