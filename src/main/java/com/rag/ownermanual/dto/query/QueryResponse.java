package com.rag.ownermanual.dto.query;

import java.util.List;

/**
 * Response body for POST /api/v1/query: answer and citations.
 * @param answer    Generated answer text (grounded in retrieved chunks).
 * @param citations List of citations (chunk refs used to generate the answer). Never null; may be empty.
 */
public record QueryResponse(
        String answer,
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
