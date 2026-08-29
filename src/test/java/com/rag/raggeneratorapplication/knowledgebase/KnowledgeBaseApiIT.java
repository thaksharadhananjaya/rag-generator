package com.rag.raggeneratorapplication.knowledgebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rag.raggeneratorapplication.TestcontainersConfiguration;
import com.rag.raggeneratorapplication.knowledgebase.repository.KnowledgeBaseRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class KnowledgeBaseApiIT {

    private static final String BASE = "/api/v1/knowledge-bases";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KnowledgeBaseRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Product Docs", "description": "  handbook  "}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Product Docs"))
                .andExpect(jsonPath("$.description").value("handbook"))
                .andExpect(jsonPath("$.embeddingModel").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        assertThat(result.getResponse().getHeader("Location")).endsWith("/" + id);
    }

    @Test
    void getReturnsCreatedKnowledgeBase() throws Exception {
        UUID id = create("Engineering");

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Engineering"));
    }

    @Test
    void listReturnsPageEnvelope() throws Exception {
        create("Alpha");
        create("Bravo");

        mockMvc.perform(get(BASE).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void duplicateNameReturns409() throws Exception {
        create("Support");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "support"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void blankNameReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void getUnknownReturns404() throws Exception {
        mockMvc.perform(get(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deleteRemovesKnowledgeBase() throws Exception {
        UUID id = create("Throwaway");

        mockMvc.perform(delete(BASE + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isNotFound());

        assertThat(repository.existsById(id)).isFalse();
    }

    private UUID create(String name) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }
}
