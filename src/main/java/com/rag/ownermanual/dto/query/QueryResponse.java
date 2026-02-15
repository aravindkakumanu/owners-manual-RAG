package com.rag.ownermanual.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response body for POST /api/v1/query: answer and citations.
 * @param answer    Generated answer text (grounded in retrieved chunks).
 * @param citations List of citations (chunk refs used to generate the answer). Never null; may be empty.
 */
@Schema(description = "Response body for POST /api/v1/query: generated answer and citations.")
public record QueryResponse(
        @Schema(description = "Generated answer text (grounded in retrieved chunks)")
        String answer,
        @Schema(description = "List of citations (chunk refs used to generate the answer). Never null; may be empty.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<Citation> citations
) {
    public QueryResponse {
        if (citations == null) {
            throw new IllegalArgumentException("citations must not be null; use empty list or QueryResponse.of()");
        }
    }

    /**
     * Builds a response; null citations are treated as empty list. Use this when the service
     * has no citations (e.g. no chunks retrieved) to avoid having to pass List.of() explicitly.
     */
    public static QueryResponse of(String answer, List<Citation> citations) {
        return new QueryResponse(answer, citations != null ? citations : List.of());
    }
}
