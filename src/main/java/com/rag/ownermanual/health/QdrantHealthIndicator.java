package com.rag.ownermanual.health;

import com.rag.ownermanual.config.QdrantVectorStoreProperties;
import io.qdrant.client.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Qdrant connectivity health check.
 *
 * <p> the boolean result (exists vs not) does not determine UP/DOWN.
 * A successful response (either true or false) means Qdrant is reachable.
 * Exceptions/timeouts mean Qdrant is unreachable.</p>
 *
 */
@Component
public class QdrantHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(QdrantHealthIndicator.class);
    private static final Duration QDRANT_CHECK_TIMEOUT = Duration.ofSeconds(3);

    private final QdrantClient qdrantClient;
    private final String collectionName;

    public QdrantHealthIndicator(QdrantClient qdrantClient, QdrantVectorStoreProperties properties) {
        this.qdrantClient = qdrantClient;
        this.collectionName = properties.getCollectionName();
    }

    @Override
    public Health health() {
        try {
            // Any successful call proves connectivity; the returned value only adds context.
            var future = qdrantClient.collectionExistsAsync(collectionName, QDRANT_CHECK_TIMEOUT);
            Boolean exists = future.get(QDRANT_CHECK_TIMEOUT.toSeconds() + 1, TimeUnit.SECONDS);
            return Health.up()
                    .withDetail("collection", collectionName)
                    .withDetail("collectionExists", exists)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Qdrant health check interrupted", e);
            return Health.down()
                    .withDetail("error", "interrupted")
                    .withException(e)
                    .build();
        } catch (TimeoutException e) {
            log.warn("Qdrant health check timed out after {}", QDRANT_CHECK_TIMEOUT);
            return Health.down()
                    .withDetail("error", "timeout")
                    .withException(e)
                    .build();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.warn("Qdrant health check failed: {}", cause != null ? cause.getMessage() : e.getMessage());
            return Health.down()
                    .withDetail("error", cause != null ? cause.getMessage() : e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
