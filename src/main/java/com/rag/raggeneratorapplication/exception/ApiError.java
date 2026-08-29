package com.rag.raggeneratorapplication.exception;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error body for every non-2xx API response.
 *
 * @param code        stable machine-readable identifier, e.g. {@code NOT_FOUND}
 * @param message     human-readable summary, safe to surface to a caller
 * @param path        request path that produced the error
 * @param timestamp   when the error was produced
 * @param fieldErrors per-field validation problems; {@code null} when not a validation error
 */
public record ApiError(
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(ErrorCode code, String message, String path) {
        return new ApiError(code.name(), message, path, Instant.now(), null);
    }

    public static ApiError of(ErrorCode code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(code.name(), message, path, Instant.now(), fieldErrors);
    }
}
