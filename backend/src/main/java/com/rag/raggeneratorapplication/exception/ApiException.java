package com.rag.raggeneratorapplication.exception;

/**
 * Base type for exceptions that map directly to an HTTP error response.
 * Thrown by application services and translated by {@code GlobalExceptionHandler}.
 */
public abstract class ApiException extends RuntimeException {

    private final transient ErrorCode errorCode;

    protected ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
