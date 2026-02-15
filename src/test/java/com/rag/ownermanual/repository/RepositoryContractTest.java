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

/**
 * Verifies that repository interfaces compile and are usable by a stub service.
 *
 * <p><b>Why this test exists:</b> Phase 1 only defines interfaces—no real DB or vector
 * store. This test proves that (1) the method signatures accept domain types correctly,
 * (2) a stub can implement both interfaces without any infrastructure, and (3) Phase 2/3
 * can depend on these contracts without changing service code. If you add a new method
 * or change a signature, this test will fail until stubs are updated—acting as a contract
 * guard.
 *
 * <p><b>What we're not testing:</b> Real search quality, persistence, or Qdrant/Postgres.
 * Those are integration tests for Phase 2/3.
 */
class RepositoryContractTest {

    @Test
    void vectorStoreRepository_stubCompilesAndReturns() {
        // Stub: no real embedding or Qdrant; just satisfies the interface.
        VectorStoreRepository repo = new VectorStoreRepository() {
            @Override
            public List<Chunk> search(float[] queryVector, String vehicleModel, int topK) {
                return List.of();
            }

            @Override
            public void upsertChunks(List<Chunk> chunks) {
                // no-op stub
            }
        };

        // Prove search(vector, vehicleModel, topK) compiles and returns; 384 = config dimension.
        List<Chunk> result = repo.search(new float[384], "Model-X", 5);
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

        // Prove save(job) and findById(id) and updateStatus(id, status, errorMessage) compile.
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
