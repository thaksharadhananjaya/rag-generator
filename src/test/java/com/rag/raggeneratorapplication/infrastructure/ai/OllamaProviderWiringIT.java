package com.rag.raggeneratorapplication.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.rag.raggeneratorapplication.TestcontainersConfiguration;
import com.rag.raggeneratorapplication.infrastructure.ai.ollama.OllamaEmbeddingService;
import com.rag.raggeneratorapplication.infrastructure.ai.ollama.OllamaLlmService;
import com.rag.raggeneratorapplication.infrastructure.ai.openai.OpenAiEmbeddingService;
import com.rag.raggeneratorapplication.infrastructure.ai.openai.OpenAiLlmService;
import com.rag.raggeneratorapplication.port.embedding.EmbeddingService;
import com.rag.raggeneratorapplication.port.llm.LlmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** With {@code app.ai.provider=ollama} the Ollama adapters replace the OpenAI ones. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "app.ai.provider=ollama")
class OllamaProviderWiringIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private LlmService llmService;

    @Test
    void ollamaAdaptersAreWired() {
        assertThat(embeddingService).isInstanceOf(OllamaEmbeddingService.class);
        assertThat(llmService).isInstanceOf(OllamaLlmService.class);
        assertThat(context.getBeanNamesForType(OpenAiEmbeddingService.class)).isEmpty();
        assertThat(context.getBeanNamesForType(OpenAiLlmService.class)).isEmpty();
        assertThat(embeddingService.modelId()).isEqualTo("nomic-embed-text");
    }
}
