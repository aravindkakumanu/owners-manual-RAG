package com.rag.ownermanual.domain;

/**
 * Unit of searchable content: a segment of text (and optional structure) that is
 * embedded and stored in the vector store.

 *
 * @param id           Unique chunk identifier (chunk_id in Qdrant); used in citations and upsert.
 * @param text         Raw text used for embedding and for snippet in citations.
 * @param manualId     Identifier of the source manual.
 * @param vehicleModel Vehicle/model; primary filter for vector search.
 * @param section      Optional heading/section title; used in citations.
 * @param page         Optional page number in the source PDF; used in citations.
 */
public record Chunk(
        String id,
        String text,
        String manualId,
        String vehicleModel,
        String section,
        Integer page
) {
    public Chunk {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id (chunk_id) must be non-blank");
        }
        if (text == null) {
            throw new IllegalArgumentException("text must be non-null");
        }
        if (manualId == null || manualId.isBlank()) {
            throw new IllegalArgumentException("manualId must be non-blank");
        }
        if (vehicleModel == null || vehicleModel.isBlank()) {
            throw new IllegalArgumentException("vehicleModel must be non-blank");
        }
    }
}
