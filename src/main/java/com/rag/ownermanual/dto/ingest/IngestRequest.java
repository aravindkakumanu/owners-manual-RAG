package com.rag.ownermanual.dto.ingest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/ingest: which manual to ingest and the document source.
 *
 * <p><b>What:</b> Controllers must use {@code @Valid @RequestBody IngestRequest} so invalid
 * requests (e.g. blank manual_id) get 400 with constraint messages before the handler runs.
 * Phase 3 will support document URL (e.g. PDF URL) first; documentContent may be added later
 * for direct upload (exactly one of documentUrl or documentContent required when both exist).
 *
 * <p><b>Why documentUrl only (no documentContent yet):</b> Phase 3 supports "PDF URL" first so we
 * avoid "exactly one of URL or content" validation until we add direct upload. When we add
 * documentContent, we can add a custom validator or @AssertTrue.
 *
 * <p><b>Why manual_id and documentUrl limits:</b> manual_id max 255 matches Postgres
 * ingestion_jobs.manual_id and keeps identifiers bounded. documentUrl max 2048 avoids
 * excessively long URLs; very large payloads may be rejected by the server or downstream
 * fetcher. Invalid manual_id format (e.g. not a valid UUID or slug) can be rejected by
 * the service and documented as an error case.
 *
 * <p>Full validation and error cases: docs/architecture/phase1-domain-and-schema.md §8.
 *
 * @param manualId     Identifier of the manual to ingest into (required). Max 255 characters.
 * @param documentUrl  URL of the document to ingest (e.g. PDF). Required for Phase 3; max 2048.
 */
public record IngestRequest(
        @NotBlank(message = "manual_id is required")
        @Size(max = 255, message = "manual_id must not exceed 255 characters")
        String manualId,

        @NotBlank(message = "document URL is required")
        @Size(max = 2048, message = "document URL must not exceed 2048 characters")
        String documentUrl
) {}
