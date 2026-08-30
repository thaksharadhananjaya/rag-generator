package com.rag.raggeneratorapplication.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rag.raggeneratorapplication.TestcontainersConfiguration;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingResult;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingService;
import com.rag.raggeneratorapplication.port.llm.LlmRequest;
import com.rag.raggeneratorapplication.port.llm.LlmResponse;
import com.rag.raggeneratorapplication.port.llm.LlmService;
import com.rag.raggeneratorapplication.port.llm.TokenUsage;
import com.rag.raggeneratorapplication.port.vector.VectorChunk;
import com.rag.raggeneratorapplication.port.vector.VectorStore;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RagApiIT.Fakes.class})
class RagApiIT {

    private static final String KB_BASE = "/api/v1/knowledge-bases";
    private static final String MODEL = "test-embedding-v1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private RecordingLlmService llm;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM chunk");
        jdbc.update("DELETE FROM document");
        jdbc.update("DELETE FROM knowledge_base");
        llm.calls.set(0);
    }

    @Test
    void queryReturnsGroundedAnswerWithSources() throws Exception {
        UUID kbId = createKnowledgeBase("Geography");
        UUID docId = insertDocument(kbId, "facts.txt");
        vectorStore.replaceDocumentChunks(kbId, docId, List.of(
                chunk(0, "Paris is the capital of France."),
                chunk(1, "Berlin is the capital of Germany.")));

        mockMvc.perform(post(KB_BASE + "/" + kbId + "/query")
                        .contentType("application/json")
                        .content("{\"question\": \"What is the capital of France?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("FAKE ANSWER grounded in [1]"))
                .andExpect(jsonPath("$.model").value("fake-llm-v1"))
                .andExpect(jsonPath("$.tokensUsed").value(15))
                .andExpect(jsonPath("$.retrievedChunks").value(2))
                .andExpect(jsonPath("$.sources.length()").value(2))
                .andExpect(jsonPath("$.sources[0].documentId").value(docId.toString()))
                .andExpect(jsonPath("$.sources[0].chunkId").isNotEmpty())
                .andExpect(jsonPath("$.sources[0].score").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0.9)));

        assertThat(llm.calls.get()).isEqualTo(1);
    }

    @Test
    void queryRespectsTopKOverride() throws Exception {
        UUID kbId = createKnowledgeBase("Topk");
        UUID docId = insertDocument(kbId, "many.txt");
        vectorStore.replaceDocumentChunks(kbId, docId, List.of(
                chunk(0, "one"), chunk(1, "two"), chunk(2, "three"), chunk(3, "four")));

        mockMvc.perform(post(KB_BASE + "/" + kbId + "/query")
                        .contentType("application/json")
                        .content("{\"question\": \"anything\", \"topK\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retrievedChunks").value(2))
                .andExpect(jsonPath("$.sources.length()").value(2));
    }

    @Test
    void queryWithNoMatchingChunksSkipsLlm() throws Exception {
        UUID kbId = createKnowledgeBase("Empty");

        mockMvc.perform(post(KB_BASE + "/" + kbId + "/query")
                        .contentType("application/json")
                        .content("{\"question\": \"What is the capital of France?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("couldn't find")))
                .andExpect(jsonPath("$.sources.length()").value(0))
                .andExpect(jsonPath("$.model").doesNotExist())
                .andExpect(jsonPath("$.retrievedChunks").value(0));

        assertThat(llm.calls.get()).isZero();
    }

    @Test
    void queryUnknownKnowledgeBaseReturns404() throws Exception {
        mockMvc.perform(post(KB_BASE + "/" + UUID.randomUUID() + "/query")
                        .contentType("application/json")
                        .content("{\"question\": \"hello\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void queryBlankQuestionReturns400() throws Exception {
        UUID kbId = createKnowledgeBase("Blank");

        mockMvc.perform(post(KB_BASE + "/" + kbId + "/query")
                        .contentType("application/json")
                        .content("{\"question\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private VectorChunk chunk(int ordinal, String text) {
        float[] vector = new float[1536];
        vector[0] = 1.0f;
        return new VectorChunk(ordinal, text, vector, MODEL, 1, null, null);
    }

    private UUID insertDocument(UUID kbId, String filename) {
        UUID docId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO document (id, knowledge_base_id, filename, size_bytes, storage_key, status)
                VALUES (?::uuid, ?::uuid, ?, ?, ?, ?)
                """, docId.toString(), kbId.toString(), filename, 10L, "key/" + docId, "COMPLETED");
        return docId;
    }

    private UUID createKnowledgeBase(String name) throws Exception {
        MvcResult result = mockMvc.perform(post(KB_BASE)
                        .contentType("application/json")
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    @TestConfiguration
    static class Fakes {

        @Bean
        @Primary
        FixedEmbeddingService fixedEmbeddingService() {
            return new FixedEmbeddingService();
        }

        @Bean
        @Primary
        RecordingLlmService recordingLlmService() {
            return new RecordingLlmService();
        }
    }

    /** Same unit vector for every text, so every stored chunk matches any query. */
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
            return MODEL;
        }

        @Override
        public int dimension() {
            return 1536;
        }
    }

    /** Canned answer; counts invocations so tests can assert the LLM was (not) called. */
    static class RecordingLlmService implements LlmService {

        final AtomicInteger calls = new AtomicInteger();

        @Override
        public LlmResponse generate(LlmRequest request) {
            calls.incrementAndGet();
            return new LlmResponse("FAKE ANSWER grounded in [1]", "fake-llm-v1",
                    new TokenUsage(10, 5, 15), "stop");
        }
    }
}
