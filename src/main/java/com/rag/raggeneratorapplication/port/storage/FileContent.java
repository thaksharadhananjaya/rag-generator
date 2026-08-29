package com.rag.raggeneratorapplication.port.storage;

import java.io.InputStream;

/**
 * A retrieved file's bytes and metadata. The caller must close {@link #content()}.
 */
public record FileContent(
        InputStream content,
        String contentType,
        long size) implements AutoCloseable {

    @Override
    public void close() throws Exception {
        content.close();
    }
}
