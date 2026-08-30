package com.rag.raggeneratorapplication.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rag.raggeneratorapplication.TestcontainersConfiguration;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingResult;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingService;
import com.rag.raggeneratorapplication.port.storage.FileContent;
import com.rag.raggeneratorapplication.port.storage.FileStorageService;
import com.rag.raggeneratorapplication.port.storage.FileUpload;
import com.rag.raggeneratorapplication.port.storage.StoredFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, DocumentApiIT.Fakes.class})
class DocumentApiIT {

    private static final String KB_BASE = "/api/v1/knowledge-bases";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM chunk");
        jdbc.update("DELETE FROM document");
        jdbc.update("DELETE FROM knowledge_base");
    }

    @Test
    void uploadIngestsDocumentToCompleted() throws Exception {
        UUID kbId = createKnowledgeBase("Docs");

        MvcResult upload = mockMvc.perform(multipart(KB_BASE + "/" + kbId + "/documents")
                        .file(textFile("notes.txt",
                                "Vector search finds nearby embeddings. "
                                        + "This document explains how retrieval augmented generation works.")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        UUID docId = UUID.fromString(JsonPath.read(upload.getResponse().getContentAsString(), "$.id"));

        awaitStatus(docId, "COMPLETED");

        mockMvc.perform(get("/api/v1/documents/" + docId))
                .andExpect(jsonPath("$.chunkCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.ingestedAt").isNotEmpty())
                .andExpect(jsonPath("$.failureReason").doesNotExist());

        Integer chunkRows = jdbc.queryForObject(
                "SELECT count(*) FROM chunk WHERE document_id = ?::uuid", Integer.class, docId.toString());
        assertThat(chunkRows).isGreaterThanOrEqualTo(1);

        String chunkModel = jdbc.queryForObject(
                "SELECT DISTINCT embedding_model FROM chunk WHERE document_id = ?::uuid",
                String.class, docId.toString());
        assertThat(chunkModel).isEqualTo("test-embedding-v1");
    }

    @Test
    void uploadToUnknownKnowledgeBaseReturns404() throws Exception {
        mockMvc.perform(multipart(KB_BASE + "/" + UUID.randomUUID() + "/documents")
                        .file(textFile("notes.txt", "hello")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void uploadEmptyFileReturns400() throws Exception {
        UUID kbId = createKnowledgeBase("Empty");

        mockMvc.perform(multipart(KB_BASE + "/" + kbId + "/documents")
                        .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void uploadDisallowedContentTypeReturns415() throws Exception {
        UUID kbId = createKnowledgeBase("Types");

        mockMvc.perform(multipart(KB_BASE + "/" + kbId + "/documents")
                        .file(new MockMultipartFile("file", "archive.zip", "application/zip", "x".getBytes())))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void listsDocumentsUnderKnowledgeBase() throws Exception {
        UUID kbId = createKnowledgeBase("Listing");
        uploadAndComplete(kbId, "a.txt");
        uploadAndComplete(kbId, "b.txt");

        mockMvc.perform(get(KB_BASE + "/" + kbId + "/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getUnknownDocumentReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/documents/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deleteRemovesDocumentAndChunks() throws Exception {
        UUID kbId = createKnowledgeBase("Deletes");
        UUID docId = uploadAndComplete(kbId, "gone.txt");

        mockMvc.perform(delete("/api/v1/documents/" + docId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/documents/" + docId)).andExpect(status().isNotFound());

        Integer chunkRows = jdbc.queryForObject(
                "SELECT count(*) FROM chunk WHERE document_id = ?::uuid", Integer.class, docId.toString());
        assertThat(chunkRows).isZero();
    }

    @Test
    void reingestReprocessesCompletedDocument() throws Exception {
        UUID kbId = createKnowledgeBase("Reingest");
        UUID docId = uploadAndComplete(kbId, "again.txt");

        mockMvc.perform(post("/api/v1/documents/" + docId + "/ingest"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        awaitStatus(docId, "COMPLETED");
    }

    private UUID uploadAndComplete(UUID kbId, String filename) throws Exception {
        MvcResult upload = mockMvc.perform(multipart(KB_BASE + "/" + kbId + "/documents")
                        .file(textFile(filename, "content of " + filename + " about embeddings and retrieval")))
                .andExpect(status().isAccepted())
                .andReturn();
        UUID docId = UUID.fromString(JsonPath.read(upload.getResponse().getContentAsString(), "$.id"));
        awaitStatus(docId, "COMPLETED");
        return docId;
    }

    private void awaitStatus(UUID docId, String expected) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/documents/" + docId))
                        .andExpect(jsonPath("$.status").value(expected)));
    }

    private UUID createKnowledgeBase(String name) throws Exception {
        MvcResult result = mockMvc.perform(post(KB_BASE)
                        .contentType("application/json")
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private static MockMultipartFile textFile(String name, String body) {
        return new MockMultipartFile("file", name, "text/plain", body.getBytes());
    }

    @TestConfiguration
    static class Fakes {

        @Bean
        @Primary
        FileStorageService inMemoryFileStorage() {
            return new InMemoryFileStorage();
        }

        @Bean
        @Primary
        EmbeddingService fixedEmbeddingService() {
            return new FixedEmbeddingService();
        }
    }

    /** Keeps uploaded bytes in memory so ingestion can read them back without MinIO. */
    static class InMemoryFileStorage implements FileStorageService {

        private final Map<String, byte[]> bytes = new ConcurrentHashMap<>();
        private final Map<String, String> types = new ConcurrentHashMap<>();

        @Override
        public StoredFile store(String storageKey, FileUpload upload) {
            try {
                byte[] data = upload.content().readAllBytes();
                bytes.put(storageKey, data);
                if (upload.contentType() != null) {
                    types.put(storageKey, upload.contentType());
                }
                return new StoredFile(storageKey, data.length, upload.contentType());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public FileContent retrieve(String storageKey) {
            byte[] data = bytes.get(storageKey);
            if (data == null) {
                throw new IllegalStateException("no such key " + storageKey);
            }
            return new FileContent(new ByteArrayInputStream(data), types.get(storageKey), data.length);
        }

        @Override
        public void delete(String storageKey) {
            bytes.remove(storageKey);
            types.remove(storageKey);
        }
    }

    /** Deterministic non-zero unit vectors of the configured dimension. */
    static class FixedEmbeddingService implements EmbeddingService {

        @Override
        public EmbeddingResult embed(List<String> texts) {
            List<float[]> vectors = texts.stream().map(t -> {
                float[] v = new float[dimension()];
                v[0] = 1.0f;
                return v;
            }).toList();
            return new EmbeddingResult(vectors, modelId(), dimension(), texts.size());
        }

        @Override
        public String modelId() {
            return "test-embedding-v1";
        }

        @Override
        public int dimension() {
            return 1536;
        }
    }
}
