package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.Chunk;

import java.util.List;

/**
 * Persistence contract for the vector store (Qdrant). QueryService searches by
 * embedding and filter; IngestionService upserts chunks after embedding.
 */
public interface VectorStoreRepository {

    /**
     * Search the vector store by query embedding and optional vehicle filter.
     * @param queryVector  Embedding of the user query (dimension must match store, e.g. 384).
     * @param vehicleModel Filter by vehicle/model; null or blank = no filter.
     * @param topK         Maximum number of chunks to return.
     * @return List of chunks ordered by similarity (nearest first); never null, may be empty.
     */
    List<Chunk> search(float[] queryVector, String vehicleModel, int topK);

    /**
     * Insert or replace chunks in the vector store. Each chunk is embedded and stored with payload.
     * @param chunks Chunks to upsert; must not be null (may be empty).
     */
    void upsertChunks(List<Chunk> chunks);
}
