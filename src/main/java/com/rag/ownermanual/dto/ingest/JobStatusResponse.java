package com.rag.ownermanual.dto.ingest;

import com.rag.ownermanual.domain.IngestionJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Response body for GET /api/v1/jobs/{id}: current job status and optional details.")
public record JobStatusResponse(
        @Schema(description = "Job identifier (same as path parameter)", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID jobId,
        @Schema(description = "Current job status: CREATED, PROCESSING, COMPLETED, FAILED", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"CREATED", "PROCESSING", "COMPLETED", "FAILED"})
        IngestionJobStatus status,
        @Schema(description = "Present when status is FAILED; null otherwise")
        String errorMessage,
        @Schema(description = "When the job was created (ISO-8601)")
        Instant createdAt,
        @Schema(description = "When the job was last updated (ISO-8601)")
        Instant updatedAt
) {}
