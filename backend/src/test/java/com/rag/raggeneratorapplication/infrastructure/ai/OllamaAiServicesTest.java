package com.rag.raggeneratorapplication.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rag.raggeneratorapplication.config.props.OllamaProperties;
import com.rag.raggeneratorapplication.exception.DependencyUnavailableException;
import com.rag.raggeneratorapplication.infrastructure.ai.ollama.OllamaEmbeddingService;
import com.rag.raggeneratorapplication.infrastructure.ai.ollama.OllamaLlmService;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingResult;
import com.rag.raggeneratorapplication.port.llm.LlmMessage;
import com.rag.raggeneratorapplication.port.llm.LlmRequest;
import com.rag.raggeneratorapplication.port.llm.LlmResponse;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Wire-format tests for the Ollama adapters against a stub HTTP server &mdash; no
 * Spring context, no real Ollama.
 */
class OllamaAiServicesTest {

    private HttpServer server;
    private RestClient restClient;
    private OllamaProperties properties;
    private final ConcurrentLinkedQueue<String> receivedBodies = new ConcurrentLinkedQueue<>();
    private volatile int status = 200;
    private volatile String responseBody = "{}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            receivedBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            try (var os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        restClient = RestClient.builder().baseUrl(baseUrl).build();
        properties = new OllamaProperties(baseUrl, "llama3.1", "nomic-embed-text", 3, 30);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void respond(int httpStatus, String body) {
        this.status = httpStatus;
        this.responseBody = body;
    }

    private String lastBody() {
        return receivedBodies.peek();
    }

    // --- embeddings -------------------------------------------------------------

    @Test
    void embedMapsBatchResponse() {
        respond(200, """
                {"model":"nomic-embed-text","embeddings":[[0.1,0.2,0.3],[0.4,0.5,0.6]],"prompt_eval_count":7}""");

        EmbeddingResult result = new OllamaEmbeddingService(restClient, properties).embed(List.of("alpha", "beta"));

        assertThat(result.model()).isEqualTo("nomic-embed-text");
        assertThat(result.dimension()).isEqualTo(3);
        assertThat(result.totalTokens()).isEqualTo(7);
        assertThat(result.vectors()).hasSize(2);
        assertThat(result.vectors().get(0)).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(result.vectors().get(1)).containsExactly(0.4f, 0.5f, 0.6f);
        assertThat(lastBody()).contains("\"model\":\"nomic-embed-text\"").contains("\"input\":[\"alpha\",\"beta\"]");
    }

    @Test
    void embedReturnsEmptyWithoutCallingServerForEmptyInput() {
        EmbeddingResult result = new OllamaEmbeddingService(restClient, properties).embed(List.of());

        assertThat(result.vectors()).isEmpty();
        assertThat(receivedBodies).isEmpty();
    }

    @Test
    void embedRaisesDependencyUnavailableOnServerError() {
        respond(500, "boom");

        assertThatThrownBy(() -> new OllamaEmbeddingService(restClient, properties).embed(List.of("x")))
                .isInstanceOf(DependencyUnavailableException.class);
    }

    @Test
    void embedRaisesWhenVectorCountDoesNotMatchInput() {
        respond(200, "{\"embeddings\":[[0.1,0.2,0.3]]}");

        assertThatThrownBy(() -> new OllamaEmbeddingService(restClient, properties).embed(List.of("x", "y")))
                .isInstanceOf(DependencyUnavailableException.class);
    }

    // --- chat ----------------------------------------------------------------

    @Test
    void generateMapsChatResponseAndUsage() {
        respond(200, """
                {"model":"llama3.1","message":{"role":"assistant","content":"Hello from Ollama"},
                 "done_reason":"stop","prompt_eval_count":11,"eval_count":4}""");

        LlmResponse response = new OllamaLlmService(restClient, properties).generate(
                LlmRequest.of(List.of(LlmMessage.system("be brief"), LlmMessage.user("hi"))));

        assertThat(response.content()).isEqualTo("Hello from Ollama");
        assertThat(response.model()).isEqualTo("llama3.1");
        assertThat(response.finishReason()).isEqualTo("stop");
        assertThat(response.usage().promptTokens()).isEqualTo(11);
        assertThat(response.usage().completionTokens()).isEqualTo(4);
        assertThat(response.usage().totalTokens()).isEqualTo(15);

        assertThat(lastBody())
                .contains("\"stream\":false")
                .contains("\"role\":\"system\"")
                .contains("\"role\":\"user\"")
                .contains("\"model\":\"llama3.1\"");
    }

    @Test
    void generateRaisesDependencyUnavailableOnServerError() {
        respond(503, "unavailable");

        assertThatThrownBy(() -> new OllamaLlmService(restClient, properties).generate(
                LlmRequest.of(List.of(LlmMessage.user("hi")))))
                .isInstanceOf(DependencyUnavailableException.class);
    }
}
