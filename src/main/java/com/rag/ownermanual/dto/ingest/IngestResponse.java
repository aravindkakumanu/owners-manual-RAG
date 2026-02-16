package com.rag.ownermanual.dto.ingest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response body for POST /api/v1/ingest: the created job identifier.
 * @param jobId UUID of the created ingestion job; use for status polling.
 */
@Schema(description = "Response body for POST /api/v1/ingest: the created job identifier.")
public record IngestResponse(
        @Schema(description = "UUID of the created ingestion job; use for status polling via GET /api/v1/jobs/{id}", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID jobId
) {}
