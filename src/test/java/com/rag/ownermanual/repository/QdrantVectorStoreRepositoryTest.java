package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.Chunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantVectorStoreRepositoryTest {

    private StubVectorStore stubVectorStore;
    private QdrantVectorStoreRepository repository;

    @BeforeEach
    void setUp() {
        stubVectorStore = new StubVectorStore();
        repository = new QdrantVectorStoreRepository(stubVectorStore);
    }

    @Test
    void search_withBlankQuery_returnsEmptyList() {
        List<Chunk> result = repository.search("   ", "Model-X", 5);

        assertThat(result).isEmpty();
        assertThat(stubVectorStore.lastSearchRequest).isNull();
    }

    @Test
    void search_withQuery_callsVectorStoreAndMapsResultsToChunks() {
        Document doc1 = new Document(
                "chunk-1",
                "Oil change interval is 5000 miles.",
                Map.of(
                        "chunk_id", "chunk-1",
                        "manual_id", "manual-1",
                        "vehicle_model", "Model-X",
                        "section", "Maintenance",
                        "page", 42
                )
        );
        stubVectorStore.searchResults = List.of(doc1);

        List<Chunk> result = repository.search("How often to change oil?", null, 5);

        assertThat(result).hasSize(1);
        Chunk chunk = result.get(0);
        assertThat(chunk.id()).isEqualTo("chunk-1");
        assertThat(chunk.text()).isEqualTo("Oil change interval is 5000 miles.");
        assertThat(chunk.manualId()).isEqualTo("manual-1");
        assertThat(chunk.vehicleModel()).isEqualTo("Model-X");
        assertThat(chunk.section()).isEqualTo("Maintenance");
        assertThat(chunk.page()).isEqualTo(42);
        assertThat(stubVectorStore.lastSearchRequest.getQuery()).isEqualTo("How often to change oil?");
        assertThat(stubVectorStore.lastSearchRequest.getTopK()).isEqualTo(5);
    }

    @Test
    void search_withVehicleModel_passesFilterToSearchRequest() {
        stubVectorStore.searchResults = List.of();

        repository.search("tire pressure", "Model-Y", 10);

        assertThat(stubVectorStore.lastSearchRequest).isNotNull();
        assertThat(stubVectorStore.lastSearchRequest.getFilterExpression()).isNotNull();
    }

    @Test
    void search_withNullVehicleModel_doesNotRequireFilter() {
        stubVectorStore.searchResults = List.of();

        repository.search("brake fluid", null, 5);

        assertThat(stubVectorStore.lastSearchRequest.getQuery()).isEqualTo("brake fluid");
        assertThat(stubVectorStore.lastSearchRequest.getTopK()).isEqualTo(5);
    }

    @Test
    void upsertChunks_mapsChunksToDocumentsAndCallsAdd() {
        Chunk chunk = new Chunk(
                "chunk-99",
                "Check coolant level monthly.",
                "manual-1",
                "Model-X",
                "Cooling",
                12
        );

        repository.upsertChunks(List.of(chunk));

        assertThat(stubVectorStore.addedDocuments).hasSize(1);
        Document doc = stubVectorStore.addedDocuments.get(0);
        assertThat(doc.getId()).isEqualTo("chunk-99");
        assertThat(doc.getText()).isEqualTo("Check coolant level monthly.");
        assertThat(doc.getMetadata())
                .containsEntry("chunk_id", "chunk-99")
                .containsEntry("manual_id", "manual-1")
                .containsEntry("vehicle_model", "Model-X")
                .containsEntry("section", "Cooling")
                .containsEntry("page", 12);
    }

    @Test
    void upsertChunks_withOptionalSectionAndPage_omitsNullFromMetadata() {
        Chunk chunk = new Chunk(
                "chunk-minimal",
                "Minimal text.",
                "manual-1",
                "Model-X",
                null,
                null
        );

        repository.upsertChunks(List.of(chunk));

        Document doc = stubVectorStore.addedDocuments.get(0);
        assertThat(doc.getMetadata())
                .containsEntry("chunk_id", "chunk-minimal")
                .containsEntry("manual_id", "manual-1")
                .containsEntry("vehicle_model", "Model-X")
                .doesNotContainKey("section")
                .doesNotContainKey("page");
    }

    @Test
    void upsertChunks_emptyList_doesNotCallAdd() {
        repository.upsertChunks(List.of());

        assertThat(stubVectorStore.addedDocuments).isEmpty();
    }

    private static final class StubVectorStore implements VectorStore {
        SearchRequest lastSearchRequest;
        List<Document> searchResults = List.of();
        final List<Document> addedDocuments = new ArrayList<>();

        @Override
        public void add(List<Document> documents) {
            addedDocuments.addAll(documents);
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            this.lastSearchRequest = request;
            return searchResults;
        }

        @Override
        public void delete(List<String> idList) {
            // no-op for tests
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
            // no-op for tests
        }
    }
}
