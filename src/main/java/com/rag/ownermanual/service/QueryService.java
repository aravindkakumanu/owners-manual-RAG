package com.rag.ownermanual.service;

import com.rag.ownermanual.config.QueryProperties;
import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.repository.VectorStoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the read path: given a text query (and optional vehicle model),
 * returns relevant chunks from the vector store.
 */
@Service
public class QueryService {

    private final VectorStoreRepository vectorStoreRepository;
    private final QueryProperties queryProperties;

    public QueryService(VectorStoreRepository vectorStoreRepository, QueryProperties queryProperties) {
        this.vectorStoreRepository = Objects.requireNonNull(vectorStoreRepository, "vectorStoreRepository");
        this.queryProperties = Objects.requireNonNull(queryProperties, "queryProperties");
    }

    /**
     * Search the vector store for chunks relevant to the query, optionally filtered by vehicle model.
     * @param queryText    User query text (embedded by the repository).
     * @param vehicleModel Optional filter; null or blank means no filter.
     * @return List of chunks ordered by similarity; never null, may be empty.
     */
    public List<Chunk> searchChunks(String queryText, String vehicleModel) {
        // Normalize blank to null so the repository contract is clear: null/blank = no filter.
        String normalizedModel = (vehicleModel != null && !vehicleModel.isBlank()) ? vehicleModel : null;
        int topK = queryProperties.getTopK();
        return vectorStoreRepository.search(queryText, normalizedModel, topK);
    }
}
