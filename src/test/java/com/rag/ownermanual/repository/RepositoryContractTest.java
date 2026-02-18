package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryContractTest {

    @Test
    void vectorStoreRepository_stubCompilesAndReturns() {
        // Stub: no real embedding or Qdrant; just satisfies the interface.
        VectorStoreRepository repo = new VectorStoreRepository() {
            @Override
            public List<Chunk> search(String queryText, String vehicleModel, int topK) {
                return List.of();
            }

            @Override
            public void upsertChunks(List<Chunk> chunks) {
                // no-op stub
            }
        };

        List<Chunk> result = repo.search("How do I change the oil?", "Model-X", 5);
        assertThat(result).isEmpty();
        repo.upsertChunks(List.of());
    }

    @Test
    void ingestionJobRepository_stubCompilesAndReturns() {
        // Stub: no Postgres; save returns the same instance, findById returns empty.
        IngestionJobRepository repo = new IngestionJobRepository() {
            @Override
            public IngestionJob save(IngestionJob job) {
                return job;
            }

            @Override
            public Optional<IngestionJob> findById(UUID id) {
                return Optional.empty();
            }

            @Override
            public void updateStatus(UUID id, IngestionJobStatus status, String errorMessage) {
                // no-op stub
            }
        };

        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(
                UUID.randomUUID(),
                "manual-1",
                IngestionJobStatus.CREATED,
                null,
                now,
                now
        );
        IngestionJob saved = repo.save(job);
        assertThat(saved).isEqualTo(job);
        assertThat(repo.findById(job.id())).isEmpty();
        repo.updateStatus(job.id(), IngestionJobStatus.PROCESSING, null);
    }
}
