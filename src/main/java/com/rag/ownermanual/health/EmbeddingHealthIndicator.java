package com.rag.ownermanual.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Embedding API health check.
 *
 */
@Component
public class EmbeddingHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingHealthIndicator.class);
    private static final String HEALTH_PROBE_TEXT = "health";

    private final EmbeddingModel embeddingModel;

    public EmbeddingHealthIndicator(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public Health health() {
        try {
            // Small, fixed input keeps token usage and latency minimal.
            float[] embedding = embeddingModel.embed(HEALTH_PROBE_TEXT);
            int dimensions = embedding != null ? embedding.length : 0;
            return Health.up()
                    .withDetail("dimensions", dimensions)
                    .build();
        } catch (Exception e) {
            log.warn("Embedding health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
