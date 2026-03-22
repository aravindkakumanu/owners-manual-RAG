package com.rag.ownermanual.controller;

import com.rag.ownermanual.TestVectorStoreConfig;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration"
)
@TestPropertySource(properties = {
        "app.security.api-key=test-api-key",
        "app.security.rate-limiting.capacity=2",
        "app.security.rate-limiting.refill-tokens=2",
        "app.security.rate-limiting.refill-period-seconds=60"
})
@Import({ GlobalExceptionHandler.class, TestVectorStoreConfig.class })
class RateLimitFlowTest {

    private static final String VALID_API_KEY = "test-api-key";

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private QueryService queryService;

    @Test
    @DisplayName("Over rate limit returns 429")
    void overLimit_returns429() {
        when(queryService.query(anyString(), isNull()))
                .thenReturn(QueryResponse.of("ok", List.of()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", VALID_API_KEY);
        String body = "{\"text\": \"q\"}";

        for (int i = 0; i < 2; i++) {
            ResponseEntity<Map<String, Object>> r = restTemplate.exchange(
                    "/api/v1/query",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            assertThat(r.getStatusCode()).as("request %d", i + 1).isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<String> third = restTemplate.exchange(
                "/api/v1/query",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
        assertThat(third.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
