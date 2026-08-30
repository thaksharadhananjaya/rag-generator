package com.rag.raggeneratorapplication.port.llm;

/** Token accounting reported by a provider, for cost logging and budgeting. */
public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {

    public static final TokenUsage UNKNOWN = new TokenUsage(0, 0, 0);
}
