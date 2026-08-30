package com.rag.raggeneratorapplication.exception;

/**
 * An external capability (LLM, embeddings, object storage, vector store) failed
 * or was unreachable. Maps to HTTP 503. Thrown by infrastructure adapters after
 * their own retries are exhausted.
 */
public class DependencyUnavailableException extends ApiException {

    public DependencyUnavailableException(String message, Throwable cause) {
        super(ErrorCode.DEPENDENCY_UNAVAILABLE, message, cause);
    }
}
