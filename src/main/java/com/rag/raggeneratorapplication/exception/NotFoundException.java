package com.rag.raggeneratorapplication.exception;

import java.util.UUID;

/** A referenced resource does not exist. Maps to HTTP 404. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public static NotFoundException of(String resource, UUID id) {
        return new NotFoundException(resource + " " + id + " not found");
    }
}
