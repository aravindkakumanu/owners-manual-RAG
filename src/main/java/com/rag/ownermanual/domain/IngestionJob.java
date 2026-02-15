package com.rag.ownermanual.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Async ingestion run: "ingest this PDF for this manual." Stored in Postgres
 * (table ingestion_jobs); returned as job_id from POST /ingest and reported via
 * GET /jobs/{id}.
 *
 * 
 * @param id           Job primary key; returned as job_id from the API.
 * @param manualId     Which manual is being ingested.
 * @param status       Lifecycle status (CREATED → PROCESSING → COMPLETED | FAILED).
 * @param errorMessage Set when status is FAILED; null otherwise.
 * @param createdAt    When the job was created.
 * @param updatedAt    When the job was last updated.
 */
public record IngestionJob(
        UUID id,
        String manualId,
        IngestionJobStatus status,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    
    public IngestionJob {
        if (id == null) {
            throw new IllegalArgumentException("id must be non-null");
        }
        if (manualId == null || manualId.isBlank()) {
            throw new IllegalArgumentException("manualId must be non-blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must be non-null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must be non-null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must be non-null");
        }
    }
}
