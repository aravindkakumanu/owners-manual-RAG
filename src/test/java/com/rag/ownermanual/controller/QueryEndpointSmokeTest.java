package com.rag.ownermanual.controller;

import com.rag.ownermanual.TestVectorStoreConfig;
import com.rag.ownermanual.dto.query.Citation;
import com.rag.ownermanual.dto.query.QueryResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Smoke test for POST /api/v1/query: endpoint is reachable and returns 200 for a minimal valid request.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration"
)
@Import({ GlobalExceptionHandler.class, TestVectorStoreConfig.class })
class QueryEndpointSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private QueryService queryService;

    /**
     * Minimal valid request (only required field: text) → 200 and response body contains
     * answer and citations. Service is mocked to return a fixed response so we don't need
     * real dependencies.
     */
    @Test
    @DisplayName("Minimal valid POST /api/v1/query returns 200 with answer and citations")
    void minimalValidRequest_returns200WithAnswerAndCitations() {
        QueryResponse mockResponse = QueryResponse.of(
                "Smoke test answer.",
                List.of(new Citation("smoke-chunk-1", "Section", "Snippet.", 1))
        );
        when(queryService.query(anyString(), isNull())).thenReturn(mockResponse);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"text\": \"smoke question\"}", headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/query",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).as("response body").isNotNull();
        assertThat(body).containsKey("answer");
        assertThat(body).containsKey("citations");
        assertThat(body.get("answer")).isEqualTo("Smoke test answer.");

        verify(queryService).query("smoke question", null);
    }
}
