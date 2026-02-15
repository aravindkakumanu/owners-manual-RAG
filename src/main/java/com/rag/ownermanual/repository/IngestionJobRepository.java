package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for ingestion jobs (Postgres). IngestionService creates
 * jobs, updates status as processing proceeds, and the job-status API reads by id.
 */
public interface IngestionJobRepository {

    /**
     * Persist a new job or return an existing one.
     * @param job Job to save; id, manualId, status, timestamps must be set.
     * @return The saved job (possibly with DB-generated fields if any); never null.
     */
    IngestionJob save(IngestionJob job);

    /**
     * Load a job by primary key.
     * @param id Job id (UUID).
     * @return The job if found; empty if not.
     */
    Optional<IngestionJob> findById(UUID id);

    /**
     * Update status (and optionally error message) for an existing job.
     * @param id           Job id.
     * @param status       New status.
     * @param errorMessage Error message when status is FAILED; null otherwise.
     */
    void updateStatus(UUID id, IngestionJobStatus status, String errorMessage);
}
