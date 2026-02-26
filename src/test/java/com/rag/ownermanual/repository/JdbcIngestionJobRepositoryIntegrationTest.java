package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.rag.ownermanual.TestVectorStoreConfig;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for JdbcIngestionJobRepository against the real ingestion_jobs table.
 */
@SpringBootTest(
        properties = "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration"
)
@Import(TestVectorStoreConfig.class)
class JdbcIngestionJobRepositoryIntegrationTest {

    @Autowired
    private IngestionJobRepository repository;

    @Test
    @DisplayName("save then findById returns the same job with all fields")
    void save_andFindById_returnsSavedJob() {
        UUID id = UUID.randomUUID();
        String manualId = "test-manual-1";
        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(
                id,
                manualId,
                IngestionJobStatus.CREATED,
                null,
                now,
                now
        );

        repository.save(job);

        Optional<IngestionJob> found = repository.findById(id);
        assertThat(found).isPresent();
        IngestionJob loaded = found.get();
        assertThat(loaded.id()).isEqualTo(id);
        assertThat(loaded.manualId()).isEqualTo(manualId);
        assertThat(loaded.status()).isEqualTo(IngestionJobStatus.CREATED);
        assertThat(loaded.errorMessage()).isNull();
        assertThat(loaded.createdAt()).isEqualTo(now);
        assertThat(loaded.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("updateStatus changes status and updated_at; FAILED sets error_message")
    void updateStatus_updatesStatusAndErrorMessage() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(
                id,
                "manual-2",
                IngestionJobStatus.CREATED,
                null,
                now,
                now
        );
        repository.save(job);

        repository.updateStatus(id, IngestionJobStatus.PROCESSING, null);

        Optional<IngestionJob> afterProcessing = repository.findById(id);
        assertThat(afterProcessing).isPresent();
        assertThat(afterProcessing.get().status()).isEqualTo(IngestionJobStatus.PROCESSING);
        assertThat(afterProcessing.get().errorMessage()).isNull();
        assertThat(afterProcessing.get().updatedAt()).isAfterOrEqualTo(now);

        repository.updateStatus(id, IngestionJobStatus.FAILED, "Parse error: not a PDF");

        Optional<IngestionJob> afterFailed = repository.findById(id);
        assertThat(afterFailed).isPresent();
        assertThat(afterFailed.get().status()).isEqualTo(IngestionJobStatus.FAILED);
        assertThat(afterFailed.get().errorMessage()).isEqualTo("Parse error: not a PDF");
    }

    @Test
    @DisplayName("findById for non-existent id returns empty")
    void findById_whenNotFound_returnsEmpty() {
        Optional<IngestionJob> found = repository.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }
}
