package com.rag.ownermanual.controller;

import com.rag.ownermanual.TestVectorStoreConfig;
import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;
import com.rag.ownermanual.dto.ingest.IngestResponse;
import com.rag.ownermanual.dto.query.Citation;
import com.rag.ownermanual.dto.query.QueryResponse;
import com.rag.ownermanual.repository.IngestionJobRepository;
import com.rag.ownermanual.service.IngestionService;
import com.rag.ownermanual.service.QueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration"
)
@Import({ GlobalExceptionHandler.class, TestVectorStoreConfig.class })
@TestPropertySource(properties = "app.security.api-key=test-api-key")
class SecurityFlowTest {

    private static final String VALID_API_KEY = "test-api-key";

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private QueryService queryService;

    @MockitoBean
    private IngestionService ingestionService;

    @MockitoBean
    private IngestionJobRepository ingestionJobRepository;

    @Test
    @DisplayName("POST /api/v1/query without X-Api-Key returns 401")
    void query_withoutApiKey_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"text\": \"hello\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/query",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/v1/query with invalid X-Api-Key returns 401")
    void query_withInvalidApiKey_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", "wrong-key");
        HttpEntity<String> entity = new HttpEntity<>("{\"text\": \"hello\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/query",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/v1/query with valid X-Api-Key returns 200 (success path)")
    void query_withValidApiKey_returns200() {
        when(queryService.query(anyString(), isNull()))
                .thenReturn(QueryResponse.of("Answer", List.of(new Citation("c1", "S", "snippet", 1))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", VALID_API_KEY);
        HttpEntity<String> entity = new HttpEntity<>("{\"text\": \"hello\"}", headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/query",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("answer");
        verify(queryService).query("hello", null);
    }

    @Test
    @DisplayName("POST /api/v1/ingest without X-Api-Key returns 401")
    void ingest_withoutApiKey_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"manualId\":\"m1\",\"documentUrl\":\"https://example.com/doc.pdf\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/ingest",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/v1/ingest with valid X-Api-Key returns 200 (success path)")
    void ingest_withValidApiKey_returns200() {
        UUID jobId = UUID.randomUUID();
        when(ingestionService.startIngestion(any())).thenReturn(new IngestResponse(jobId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", VALID_API_KEY);
        String body = "{\"manualId\":\"m1\",\"documentUrl\":\"https://example.com/doc.pdf\"}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/ingest",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("jobId");
        verify(ingestionService).startIngestion(any());
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{id} without X-Api-Key returns 401")
    void getJobStatus_withoutApiKey_returns401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/jobs/" + UUID.randomUUID(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{id} with valid X-Api-Key returns 200 when job exists")
    void getJobStatus_withValidApiKey_returns200() {
        UUID jobId = UUID.randomUUID();
        IngestionJob job = new IngestionJob(jobId, "m1", IngestionJobStatus.COMPLETED, null, Instant.now(), Instant.now());
        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", VALID_API_KEY);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "COMPLETED");
    }

}
