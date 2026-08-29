package com.rag.raggeneratorapplication.config.props;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Upload validation policy for the document module.
 *
 * @param allowedContentTypes MIME types accepted for upload; an empty list means
 *                            "accept any type" and defers to the text extractor
 */
@ConfigurationProperties(prefix = "app.document")
public record DocumentProperties(List<String> allowedContentTypes) {

    private static final List<String> DEFAULT_ALLOWED = List.of(
            "application/pdf",
            "text/plain",
            "text/html",
            "text/csv");

    public DocumentProperties {
        allowedContentTypes = (allowedContentTypes == null || allowedContentTypes.isEmpty())
                ? DEFAULT_ALLOWED
                : List.copyOf(allowedContentTypes);
    }

    /** {@code true} if uploads of the given content type are accepted. */
    public boolean accepts(String contentType) {
        return contentType != null
                && allowedContentTypes.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(contentType));
    }
}
