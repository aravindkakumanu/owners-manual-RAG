package com.rag.ownermanual.dto.ingest;

import com.rag.ownermanual.domain.IngestionJobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for GET /api/v1/jobs/{id}: current job status and optional details.
 *
 * <p><b>What:</b> Returned when the client polls for ingest job status. job_id and status
 * are always present; error_message is set when status is FAILED; timestamps are optional
 * for auditing and display. Status values: CREATED, PROCESSING, COMPLETED, FAILED (see
 * {@link IngestionJobStatus}).
 *
 * <p><b>Why use domain IngestionJobStatus here:</b> One enum for domain, DB, and API—no separate
 * "API status" type to keep in sync. JSON exposes CREATED, PROCESSING, COMPLETED, FAILED as strings.
 *
 * <p><b>Why optional error_message and timestamps:</b> error_message only meaningful when
 * status is FAILED. Timestamps may be omitted in a minimal response or included for UX
 * ("started at X, updated at Y"). Null is serialized as absent in JSON so the contract
 * stays simple.
 *
 * <p>Validation and error cases: docs/architecture/phase1-domain-and-schema.md §8.
 *
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
