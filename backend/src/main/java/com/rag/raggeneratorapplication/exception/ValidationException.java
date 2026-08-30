package com.rag.raggeneratorapplication.exception;

/**
 * A request is well-formed but semantically invalid (e.g. an empty upload).
 * Maps to HTTP 400. For bean-validation failures the framework path in
 * {@code GlobalExceptionHandler} is used instead.
 */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
