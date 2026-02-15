package com.rag.ownermanual.dto.ingest;

import com.rag.ownermanual.domain.IngestionJobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for GET /api/v1/jobs/{id}: current job status and optional details.
 * @param jobId        Same as path parameter; returned for convenience and consistency.
 * @param status       Current job status (CREATED, PROCESSING, COMPLETED, FAILED).
 * @param errorMessage Present when status is FAILED; null otherwise.
 * @param createdAt    When the job was created (optional; ISO-8601 when present).
 * @param updatedAt    When the job was last updated (optional; ISO-8601 when present).
 */
public record JobStatusResponse(
        UUID jobId,
        IngestionJobStatus status,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {}
