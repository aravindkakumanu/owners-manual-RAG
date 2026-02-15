package com.rag.ownermanual.dto.ingest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/ingest: which manual to ingest and the document source.
 * @param manualId     Identifier of the manual to ingest into (required). Max 255 characters.
 * @param documentUrl  URL of the document to ingest (e.g. PDF). max 2048.
 */
@Schema(description = "Request body for POST /api/v1/ingest: manual identifier and document URL.")
public record IngestRequest(
        @NotBlank(message = "manual_id is required")
        @Size(max = 255, message = "manual_id must not exceed 255 characters")
        @Schema(description = "Identifier of the manual to ingest into (required)", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
        String manualId,

        @NotBlank(message = "document URL is required")
        @Size(max = 2048, message = "document URL must not exceed 2048 characters")
        @Schema(description = "URL of the document to ingest (e.g. PDF)", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
        String documentUrl
) {}
