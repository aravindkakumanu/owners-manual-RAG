package com.rag.ownermanual.repository;

import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.seed.SeedChunks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "QDRANT_URL", matches = ".+")
class SeedAndReadPathIntegrationTest {

    @Autowired
    private VectorStoreRepository vectorStoreRepository;

    @BeforeEach
    void seedVectorStore() {
        vectorStoreRepository.upsertChunks(SeedChunks.getSeedChunks());
    }

    @Test
    void search_withMatchingQuery_returnsExpectedChunk() {
        List<Chunk> results = vectorStoreRepository.search("oil change interval", null, 10);

        assertThat(results).isNotEmpty();
        assertThat(results.stream().map(Chunk::id).toList()).contains("seed-chunk-1");
        Chunk oilChunk = results.stream().filter(c -> "seed-chunk-1".equals(c.id())).findFirst().orElseThrow();
        assertThat(oilChunk.text()).contains("5000 miles");
        assertThat(oilChunk.manualId()).isEqualTo(SeedChunks.SEED_MANUAL_ID);
        assertThat(oilChunk.vehicleModel()).isEqualTo(SeedChunks.SEED_VEHICLE_MODEL);
    }

    @Test
    void search_withCorrectVehicleModelFilter_returnsSeedChunks() {
        List<Chunk> results = vectorStoreRepository.search("oil change interval", SeedChunks.SEED_VEHICLE_MODEL, 10);

        assertThat(results).isNotEmpty();
        assertThat(results.stream().map(Chunk::id).toList()).contains("seed-chunk-1");
    }

    @Test
    void search_withWrongVehicleModelFilter_returnsNoSeedChunks() {
        List<Chunk> results = vectorStoreRepository.search("oil change interval", "other-model", 10);

        // Seed chunks all have vehicle_model mvp-manual; filter "other-model" must exclude them.
        boolean anySeedChunk = results.stream()
                .anyMatch(c -> SeedChunks.getSeedChunks().stream().map(Chunk::id).anyMatch(id -> id.equals(c.id())));
        assertThat(anySeedChunk).as("no seed chunk should be returned when filtering by other-model").isFalse();
    }
}
