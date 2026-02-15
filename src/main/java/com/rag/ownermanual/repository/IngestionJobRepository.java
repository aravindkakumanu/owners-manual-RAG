package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for ingestion jobs (Postgres). IngestionService creates
 * jobs, updates status as processing proceeds, and the job-status API reads by id.
 *
 * <p><b>What:</b> Interface only; implementation (Phase 2/3) will use JPA or JDBC
 * and map IngestionJob ↔ ingestion_jobs table. No implementation in Phase 1.
 *
 * <p><b>Why interface first:</b> Dependency inversion—IngestionService and the
 * job-status controller depend on this contract. Implementation can be swapped
 * (e.g. in-memory stub for tests, JPA for production).
 *
 * @see com.rag.ownermanual.domain.IngestionJob
 * @see com.rag.ownermanual.domain.IngestionJobStatus
 * @see docs/architecture/phase1-domain-and-schema.md §2.1, §5
 */
public interface IngestionJobRepository {

    /**
     * Persist a new job or return an existing one (implementation-defined for
     * idempotency).
     *
     * <p><b>When used:</b> POST /ingest creates a job in CREATED state; caller gets
     * job_id from the returned instance.
     *
     * @param job Job to save; id, manualId, status, timestamps must be set.
     * @return The saved job (possibly with DB-generated fields if any); never null.
     */
    IngestionJob save(IngestionJob job);

    /**
     * Load a job by primary key.
     *
     * <p><b>When used:</b> GET /jobs/{id} returns status (and optional error_message,
     * timestamps) for polling after ingest.
     *
     * @param id Job id (UUID).
     * @return The job if found; empty if not.
     */
    Optional<IngestionJob> findById(UUID id);

    /**
     * Update status (and optionally error message) for an existing job.
     *
     * <p><b>When used:</b> Ingestion pipeline moves job to PROCESSING, then COMPLETED
     * or FAILED; errorMessage is set when FAILED. Void return keeps the API simple;
     * caller can findById if the updated job is needed.
     *
     * @param id           Job id.
     * @param status       New status.
     * @param errorMessage Error message when status is FAILED; null otherwise.
     */
    void updateStatus(UUID id, IngestionJobStatus status, String errorMessage);
}
