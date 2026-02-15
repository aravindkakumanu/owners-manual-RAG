package com.rag.ownermanual.dto.query;

/**
 * One citation in a query response: reference to a chunk that was used to generate the answer.
 * Enables the client to show "from section X, page Y" or to link back to the source.
 * @param chunkId  Identifier of the chunk (matches Chunk.id / Qdrant chunk_id). Required for citation identity.
 * @param section  Optional heading/section title from the source manual.
 * @param snippet  Optional short excerpt of the chunk text for display.
 * @param page     Optional page number in the source PDF.
 */
public record Citation(
        String chunkId,
        String section,
        String snippet,
        Integer page
) {
    public Citation {
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId must be non-blank");
        }
    }
}
