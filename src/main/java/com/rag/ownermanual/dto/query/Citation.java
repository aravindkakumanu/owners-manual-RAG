package com.rag.ownermanual.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One citation in a query response: reference to a chunk that was used to generate the answer.
 * Enables the client to show "from section X, page Y" or to link back to the source.
 * @param chunkId  Identifier of the chunk (matches Chunk.id / Qdrant chunk_id). Required for citation identity.
 * @param section  Optional heading/section title from the source manual.
 * @param snippet  Optional short excerpt of the chunk text for display.
 * @param page     Optional page number in the source PDF.
 */
@Schema(description = "One citation: reference to a chunk used to generate the answer.")
public record Citation(
        @Schema(description = "Identifier of the chunk (required)", requiredMode = Schema.RequiredMode.REQUIRED)
        String chunkId,
        @Schema(description = "Optional heading/section title from the source manual")
        String section,
        @Schema(description = "Optional short excerpt of the chunk text for display")
        String snippet,
        @Schema(description = "Optional page number in the source PDF")
        Integer page
) {
    public Citation {
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId must be non-blank");
        }
    }
}
