package com.rag.ownermanual;

import io.qdrant.client.QdrantClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.google.common.util.concurrent.Futures;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration that provides stub VectorStore and QdrantClient beans
 * so full application context can load without a real Qdrant instance (e.g. for
 * OwnerManualRagApplicationTests and HealthSmokeTest).
 */
@TestConfiguration
public class TestVectorStoreConfig {

    @Bean
    @Primary
    public VectorStore vectorStore() {
        return new VectorStore() {
            @Override
            public void add(List<Document> documents) {}

            @Override
            public List<Document> similaritySearch(SearchRequest request) {
                return List.of();
            }

            @Override
            public void delete(List<String> idList) {}

            @Override
            public void delete(Filter.Expression filterExpression) {}
        };
    }

    @Bean
    @Primary
    public QdrantClient qdrantClient() {
        QdrantClient client = mock(QdrantClient.class);
        when(client.collectionExistsAsync(anyString(), any(Duration.class)))
                .thenReturn(Futures.immediateFuture(true));
        return client;
    }
}
