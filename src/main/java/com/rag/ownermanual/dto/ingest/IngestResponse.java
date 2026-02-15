package com.rag.ownermanual.dto.ingest;

import java.util.UUID;

/**
 * Response body for POST /api/v1/ingest: the created job identifier.
 * @param jobId UUID of the created ingestion job; use for status polling.
 */
public record IngestResponse(UUID jobId) {}
