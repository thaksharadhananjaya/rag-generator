package com.rag.raggeneratorapplication.exception;

/** The uploaded file's content type is not accepted for ingestion. Maps to HTTP 415. */
public class UnsupportedMediaTypeException extends ApiException {

    public UnsupportedMediaTypeException(String message) {
        super(ErrorCode.UNSUPPORTED_MEDIA_TYPE, message);
    }
}
