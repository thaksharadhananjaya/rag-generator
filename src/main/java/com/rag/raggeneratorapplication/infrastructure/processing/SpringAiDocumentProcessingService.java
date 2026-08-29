package com.rag.raggeneratorapplication.infrastructure.processing;

import com.rag.raggeneratorapplication.config.props.ProcessingProperties;
import com.rag.raggeneratorapplication.exception.DependencyUnavailableException;
import com.rag.raggeneratorapplication.port.processing.DocumentProcessingService;
import com.rag.raggeneratorapplication.port.processing.ProcessedChunk;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * {@link DocumentProcessingService} backed by Spring AI: a document reader
 * ({@link PagePdfDocumentReader} for PDF, {@link TikaDocumentReader} for
 * everything else) feeding a {@link TokenTextSplitter}. No extraction or chunking
 * logic is implemented here &mdash; only reader selection and result mapping.
 */
@Component
public class SpringAiDocumentProcessingService implements DocumentProcessingService {

    private final TokenTextSplitter splitter;
    private final int chunkSizeTokens;

    public SpringAiDocumentProcessingService(ProcessingProperties properties) {
        this.chunkSizeTokens = properties.chunkSizeTokens();
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(properties.chunkSizeTokens())
                .withMinChunkSizeChars(properties.minChunkSizeChars())
                .build();
    }

    @Override
    public List<ProcessedChunk> process(String filename, InputStream content) {
        byte[] bytes = readAll(content, filename);
        Resource resource = new NamedByteArrayResource(bytes, filename);
        //
        List<Document> pagePdfDocumentReader= new PagePdfDocumentReader(resource).get();
        List<Document> chunks = splitter.apply(pagePdfDocumentReader);
        List<ProcessedChunk> result = new ArrayList<>(chunks.size());
        //
        int ordinal = 0;
        for (Document chunk : chunks) {
            String text = chunk.getText();
            if (text != null && !text.isBlank()) {
                result.add(new ProcessedChunk(ordinal++, text.strip(), pageNumber(chunk)));
            }
        }
        return result;
    }

    @Override
    public String configId() {
        return "spring-ai;reader=page-pdf|tika;splitter=token;chunk=" + chunkSizeTokens;
    }

    private static Integer pageNumber(Document chunk) {
        Object page = chunk.getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER);
        if (page instanceof Number number) {
            return number.intValue();
        }
        if (page instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static byte[] readAll(InputStream content, String filename) {
        try {
            return content.readAllBytes();
        } catch (IOException e) {
            throw new DependencyUnavailableException("Failed reading uploaded file '" + filename + "'", e);
        }
    }

    /** {@link ByteArrayResource} that reports a filename, so readers can detect the type. */
    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
