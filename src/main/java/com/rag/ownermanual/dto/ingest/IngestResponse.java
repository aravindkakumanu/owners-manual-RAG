package com.rag.ownermanual.dto.ingest;

import java.util.UUID;

/**
 * Response body for POST /api/v1/ingest: the created job identifier.
 *
 * <p><b>What:</b> Returned immediately after accepting an ingest request. The client uses
 * job_id to poll GET /api/v1/jobs/{id} for status. Serializes to JSON as a string (e.g.
 * "550e8400-e29b-41d4-a716-446655440000").
 *
 * <p><b>Why UUID:</b> Matches domain {@link com.rag.ownermanual.domain.IngestionJob#id}
 * and Postgres ingestion_jobs.id. Type-safe in Java; standard string representation in the API.
 *
 * @param jobId UUID of the created ingestion job; use for status polling.
 */
public record IngestResponse(UUID jobId) {}
