package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.Chunk;

import java.util.List;

/**
 * Persistence contract for the vector store (Qdrant). QueryService searches by
 * embedding and filter; IngestionService upserts chunks after embedding.
 *
 * <p><b>What:</b> Interface only; implementation (Phase 2/3) will use Spring AI
 * VectorStore and map Chunk ↔ Qdrant payload (chunk_id, text, manual_id,
 * vehicle_model, section, page). No implementation in Phase 1.
 *
 * <p><b>Why interface first:</b> Dependency inversion—services depend on this
 * contract, not on Qdrant or Spring AI. Phase 2 implements search; Phase 3
 * implements upsert. Stubs can satisfy the interface for tests.
 *
 * @see com.rag.ownermanual.domain.Chunk
 * @see docs/architecture/phase1-domain-and-schema.md §3 (Qdrant payload)
 * @see docs/architecture/component-overview.md
 */
public interface VectorStoreRepository {

    /**
     * Search the vector store by query embedding and optional vehicle filter.
     *
     * <p><b>Why queryVector:</b> QueryService will embed the user's question with the
     * same model used at ingest; dimension (e.g. 384) must match the store config.
     * <p><b>Why vehicleModel filter:</b> Per validation-decisions, we filter by model
     * so answers and citations are scoped to the right manual; null/blank = no filter.
     *
     * @param queryVector  Embedding of the user query (dimension must match store, e.g. 384).
     * @param vehicleModel Filter by vehicle/model; null or blank = no filter (implementation-defined).
     * @param topK         Maximum number of chunks to return.
     * @return List of chunks ordered by similarity (nearest first); never null, may be empty.
     */
    List<Chunk> search(float[] queryVector, String vehicleModel, int topK);

    /**
     * Insert or replace chunks in the vector store. Each chunk is embedded (by the
     * caller or implementation) and stored with payload (id, text, manualId,
     * vehicleModel, section, page).
     *
     * <p><b>Why upsert:</b> Re-ingesting the same manual can replace chunks by id
     * instead of duplicating; idempotent per chunk id if store uses chunk_id as point id.
     *
     * @param chunks Chunks to upsert; must not be null (may be empty).
     */
    void upsertChunks(List<Chunk> chunks);
}
