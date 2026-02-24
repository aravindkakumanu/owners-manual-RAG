package com.rag.ownermanual.controller;

import com.rag.ownermanual.repository.VectorStoreRepository;
import com.rag.ownermanual.seed.SeedChunks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full vertical-slice integration test: seed → POST /api/v1/query → 200,
 * non-empty answer, and at least one citation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "QDRANT_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
class QueryApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VectorStoreRepository vectorStoreRepository;

    /**
     * Seed the vector store before each test so the query can retrieve relevant chunks.
     */
    @BeforeEach
    void seedVectorStore() {
        vectorStoreRepository.upsertChunks(SeedChunks.getSeedChunks());
    }

    /**
     * Full slice: valid request with a query that matches seeded content (oil change) and
     * optional vehicle model filter. Asserts 200, non-empty answer, and at least one citation.
     */
    @Test
    @DisplayName("POST /api/v1/query with seeded data returns 200, non-empty answer, and ≥1 citation")
    void fullSlice_postQuery_returns200WithAnswerAndCitations() {
        String requestBody = """
                {"text": "What is the oil change interval?", "vehicleModel": "%s"}
                """.formatted(SeedChunks.SEED_VEHICLE_MODEL);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/query",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).as("response body").isNotNull();

        assertThat(body.get("answer")).as("answer").isNotNull();
        assertThat(body.get("answer").toString().trim()).as("answer non-empty").isNotEmpty();

        @SuppressWarnings("unchecked")
        List<Object> citations = (List<Object>) body.get("citations");
        assertThat(citations).as("citations list").isNotNull();
        assertThat(citations).as("at least one citation when chunks were retrieved and sent to LLM").isNotEmpty();
    }
}
