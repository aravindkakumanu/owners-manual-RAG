package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vector store repository implementation using Spring AI's Qdrant VectorStore.
 */
@Component
public class QdrantVectorStoreRepository implements VectorStoreRepository {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreRepository.class);

    /** Metadata keys matching Qdrant payload */
    private static final String META_CHUNK_ID = "chunk_id";
    private static final String META_MANUAL_ID = "manual_id";
    private static final String META_VEHICLE_MODEL = "vehicle_model";
    private static final String META_SECTION = "section";
    private static final String META_PAGE = "page";

    private final VectorStore vectorStore;

    public QdrantVectorStoreRepository(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Embeds queryText via the store, runs top-k similarity search (optionally filtered by
     * vehicle_model), maps returned Documents to Chunks. A blank query returns empty to avoid
     * calling the store with no real query.
     */
    @Override
    public List<Chunk> search(String queryText, String vehicleModel, int topK) {
        if (queryText == null || queryText.isBlank()) {
            log.warn("search called with blank queryText; returning empty list");
            return List.of();
        }

        var requestBuilder = SearchRequest.builder()
                .query(queryText)  // Store embeds this internally via its EmbeddingModel
                .topK(topK);

        // Only apply vehicle_model filter when caller specified one; null/blank = search all models.
        if (vehicleModel != null && !vehicleModel.isBlank()) {
            var filter = new FilterExpressionBuilder().eq(META_VEHICLE_MODEL, vehicleModel).build();
            requestBuilder.filterExpression(filter);
        }

        List<Document> documents = vectorStore.similaritySearch(requestBuilder.build());
        return documents.stream()
                .map(this::documentToChunk)
                .toList();
    }

    /**
     * Converts chunks to Documents (id = chunk.id()), calls VectorStore.add() so the store embeds and stores them.
     */
    @Override
    public void upsertChunks(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        // Map to Documents with id = chunk.id(); VectorStore.add() + embedding is done by the store.
        List<Document> documents = chunks.stream()
                .map(this::chunkToDocument)
                .toList();
        vectorStore.add(documents);
        log.debug("Upserted {} chunks to vector store", documents.size());
    }

    /**
     * Maps a Spring AI Document to a Chunk (id from doc id, text from content,
     * manualId/vehicleModel/section/page from metadata).
     */
    private Chunk documentToChunk(Document doc) {
        Map<String, Object> m = doc.getMetadata();
        String manualId = getString(m, META_MANUAL_ID);
        String vehicleModel = getString(m, META_VEHICLE_MODEL);
        String section = getString(m, META_SECTION);
        Integer page = getInteger(m, META_PAGE);

        // Required Chunk fields cannot be null/blank; use placeholders for bad or legacy payloads.
        String id = doc.getId() != null && !doc.getId().isBlank() ? doc.getId() : getString(m, META_CHUNK_ID);
        if (id == null || id.isBlank()) {
            id = "unknown";
        }
        String text = doc.getText();
        if (text == null) {
            text = getString(m, "text");
        }
        if (text == null) {
            text = "";
        }
        if (manualId == null || manualId.isBlank()) {
            manualId = "unknown";
        }
        if (vehicleModel == null || vehicleModel.isBlank()) {
            vehicleModel = "unknown";
        }

        return new Chunk(id, text, manualId, vehicleModel, section, page);
    }

    /**
     * Maps a Chunk to Spring AI Document (id = chunk.id(), content = chunk.text(),
     * metadata = chunk_id, manual_id, vehicle_model, section if set, page if set).
     */
    private Document chunkToDocument(Chunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_CHUNK_ID, chunk.id());
        metadata.put(META_MANUAL_ID, chunk.manualId());
        metadata.put(META_VEHICLE_MODEL, chunk.vehicleModel());
        if (chunk.section() != null) {
            metadata.put(META_SECTION, chunk.section());
        }
        if (chunk.page() != null) {
            metadata.put(META_PAGE, chunk.page());
        }

        return new Document(chunk.id(), chunk.text(), metadata);
    }

    /** Returns metadata value as string because Qdrant/store may return different value types. */
    private static String getString(Map<String, Object> m, String key) {
        if (m == null) {
            return null;
        }
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }

    /** Returns metadata value as Integer because the payload may store page as Number or string. */
    private static Integer getInteger(Map<String, Object> m, String key) {
        if (m == null) {
            return null;
        }
        Object v = m.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
