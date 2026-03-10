package com.rag.ownermanual.controller;

import com.rag.ownermanual.TestVectorStoreConfig;
import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;
import com.rag.ownermanual.dto.ingest.IngestResponse;
import com.rag.ownermanual.repository.IngestionJobRepository;
import com.rag.ownermanual.service.IngestionService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Smoke tests for the ingestion API endpoints: POST /api/v1/ingest and GET /api/v1/jobs/{id}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration"
)
@Import({ GlobalExceptionHandler.class, TestVectorStoreConfig.class })
class IngestEndpointSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private IngestionService ingestionService;

    @MockitoBean
    private IngestionJobRepository ingestionJobRepository;

    @Test
    @DisplayName("Minimal valid POST /api/v1/ingest returns 200 with jobId and triggers async processing")
    void minimalValidIngestRequest_returns200WithJobId_andTriggersAsync() {
        UUID jobId = UUID.randomUUID();
        when(ingestionService.startIngestion(any())).thenReturn(new IngestResponse(jobId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"manualId":"manual-123","documentUrl":"https://example.com/manual.pdf"}
                """;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/ingest",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).as("response body").isNotNull();
        assertThat(responseBody).containsKey("jobId");
        assertThat(responseBody.get("jobId")).isEqualTo(jobId.toString());

        verify(ingestionService).startIngestion(any());
        verify(ingestionService).processJobAsync(jobId, "https://example.com/manual.pdf");
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{id} with existing job returns 200 with status payload")
    void getJobStatus_existingJob_returns200WithStatus() {
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(jobId, "manual-123", IngestionJobStatus.PROCESSING, null, now, now);
        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).as("response body").isNotNull();
        assertThat(body.get("jobId")).isEqualTo(jobId.toString());
        assertThat(body.get("status")).isEqualTo("PROCESSING");

        verify(ingestionJobRepository).findById(jobId);
        verify(ingestionService, never()).startIngestion(any());
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{id} with missing job returns 404 and ApiErrorResponse")
    void getJobStatus_missingJob_returns404WithApiErrorResponse() {
        UUID jobId = UUID.randomUUID();
        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/jobs/" + jobId,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, Object> body = response.getBody();
        assertThat(body).as("response body").isNotNull();
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("message")).as("error message").isNotNull();

        verify(ingestionJobRepository).findById(jobId);
        verify(ingestionService, never()).startIngestion(any());
    }
}

