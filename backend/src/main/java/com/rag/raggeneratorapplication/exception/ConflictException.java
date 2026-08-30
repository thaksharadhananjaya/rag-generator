package com.rag.raggeneratorapplication.exception;

/** The request conflicts with the current state of a resource. Maps to HTTP 409. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
