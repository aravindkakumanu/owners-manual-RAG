package com.rag.ownermanual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Smoke test that calls /actuator/health and asserts that:
     * <ul>
     *     <li>HTTP status is 200 OK</li>
     *     <li>Overall health status is UP</li>
     * </ul>
     *
     * <p>The EnabledIfEnvironmentVariable annotations make this test run
     * only when you have configured the expected env vars:</p>
     * <ul>
    *     <li> OPENAI_API_KEY </li>
     *     <li> GROQ_API_KEY, GROQ_MODEL </li>
     *     <li> QDRANT_URL </li>
     *     <li> SUPABASE_DB_URL, SUPABASE_DB_USERNAME, SUPABASE_DB_PASSWORD </li>
     *   </ul>
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "GROQ_MODEL", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "QDRANT_URL", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "SUPABASE_DB_USERNAME", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "SUPABASE_DB_PASSWORD", matches = ".+")
    void healthEndpointIsUpWhenDependenciesAreAvailable() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertThat(body).as("health body").isNotNull();
        assertThat(body.get("status")).as("overall health status").isEqualTo("UP");
    }
}

