package com.rag.ownermanual.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Ensures the Qdrant collection has payload indexes for every payload field we use in search filters.
 */
@Component
@Order(100) // Run after main beans; not critical path for health
public class QdrantPayloadIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QdrantPayloadIndexInitializer.class);

    /** Must match payload key used in QdrantVectorStoreRepository */
    private static final String VEHICLE_MODEL_FIELD = "vehicle_model";
    private static final Duration INDEX_OPERATION_TIMEOUT = Duration.ofSeconds(10);

    private final QdrantClient qdrantClient;
    private final String collectionName;

    public QdrantPayloadIndexInitializer(QdrantClient qdrantClient,
                                          QdrantVectorStoreProperties properties) {
        this.qdrantClient = qdrantClient;
        this.collectionName = properties.getCollectionName();
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureVehicleModelIndex();
        } catch (Exception e) {
            // Don't fail startup: collection might not exist yet (e.g. before first seed).
            log.warn("Could not ensure payload index for {} on collection {}: {}. " +
                            "Filtered search by vehicle_model may fail until the index exists.",
                    VEHICLE_MODEL_FIELD, collectionName, e.getMessage());
        }
    }

    /**
     * Creates the vehicle_model keyword index if missing. Idempotent: if the index already exists,
     * Qdrant may return an error we treat as success (already exists).
     */
    private void ensureVehicleModelIndex() throws InterruptedException, ExecutionException, TimeoutException {
        try {
            var future = qdrantClient.createPayloadIndexAsync(
                    collectionName,
                    VEHICLE_MODEL_FIELD,
                    PayloadSchemaType.Keyword,
                    null,  // indexParams: null for keyword is valid
                    true,  // wait for index to be created
                    null,
                    INDEX_OPERATION_TIMEOUT
            );
            if (future == null) {
                log.debug("Qdrant client returned null from createPayloadIndexAsync; skipping (e.g. test stub).");
                return;
            }
            future.get(INDEX_OPERATION_TIMEOUT.toSeconds() + 2, TimeUnit.SECONDS);
            log.info("Payload index ensured for field '{}' on collection '{}' (keyword).",
                    VEHICLE_MODEL_FIELD, collectionName);
        } catch (ExecutionException e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            // Index already exists or collection not found → treat as non-fatal (idempotent / not yet created).
            if (msg != null && (msg.contains("already exists") || msg.contains("AlreadyExists")
                    || msg.contains("NOT_FOUND") || msg.contains("not found"))) {
                log.debug("Payload index for '{}' on collection '{}': {} (skipping).",
                        VEHICLE_MODEL_FIELD, collectionName, msg);
                return;
            }
            throw e;
        }
    }
}
