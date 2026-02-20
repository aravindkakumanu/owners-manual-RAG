package com.rag.ownermanual.service;

import com.rag.ownermanual.config.QueryProperties;
import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.repository.VectorStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private VectorStoreRepository vectorStoreRepository;

    private QueryProperties queryProperties;
    private QueryService queryService;

    @BeforeEach
    void setUp() {
        queryProperties = new QueryProperties();
        queryProperties.setTopK(5);
        queryService = new QueryService(vectorStoreRepository, queryProperties);
    }

    /** Service is a pass-through: returned list must be the same instance the repo returns. */
    @Test
    void searchChunks_returnsListFromRepository() {
        List<Chunk> expected = List.of(
                new Chunk("c1", "Oil change every 5000 miles.", "manual-1", "Model-X", "Maintenance", 10)
        );
        when(vectorStoreRepository.search(eq("How often oil change?"), isNull(), eq(5)))
                .thenReturn(expected);

        List<Chunk> result = queryService.searchChunks("How often oil change?", null);

        assertThat(result).isSameAs(expected);
        verify(vectorStoreRepository).search("How often oil change?", null, 5);
    }

    /** Non-blank vehicleModel must be passed to the repo so Qdrant can filter by metadata. */
    @Test
    void searchChunks_withVehicleModel_passesFilterAndUsesTopK() {
        List<Chunk> expected = List.of(
                new Chunk("c1", "Tire pressure 32 psi.", "manual-1", "Model-Y", "Tires", 5)
        );
        when(vectorStoreRepository.search(eq("tire pressure"), eq("Model-Y"), eq(5)))
                .thenReturn(expected);

        List<Chunk> result = queryService.searchChunks("tire pressure", "Model-Y");

        assertThat(result).isSameAs(expected);
        verify(vectorStoreRepository).search("tire pressure", "Model-Y", 5);
    }

    /** Blank/whitespace vehicleModel → null so repo does not apply a filter (per interface contract). */
    @Test
    void searchChunks_withBlankVehicleModel_passesNullToRepository() {
        when(vectorStoreRepository.search(anyString(), isNull(), anyInt())).thenReturn(List.of());

        queryService.searchChunks("query", "   ");

        verify(vectorStoreRepository).search(eq("query"), isNull(), eq(5));
    }

    @Test
    void searchChunks_usesConfiguredTopK() {
        queryProperties.setTopK(10);
        queryService = new QueryService(vectorStoreRepository, queryProperties);
        when(vectorStoreRepository.search(anyString(), isNull(), eq(10))).thenReturn(List.of());

        queryService.searchChunks("q", null);

        verify(vectorStoreRepository).search("q", null, 10);
    }
}
