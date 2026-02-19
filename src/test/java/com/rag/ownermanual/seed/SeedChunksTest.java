package com.rag.ownermanual.seed;

import com.rag.ownermanual.domain.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeedChunksTest {

    @Test
    void getSeedChunks_returnsFiveChunks_withRequiredFields() {
        List<Chunk> chunks = SeedChunks.getSeedChunks();

        assertThat(chunks).hasSize(5);
        for (Chunk c : chunks) {
            assertThat(c.id()).isNotBlank();
            assertThat(c.text()).isNotNull();
            assertThat(c.manualId()).isEqualTo(SeedChunks.SEED_MANUAL_ID);
            assertThat(c.vehicleModel()).isEqualTo(SeedChunks.SEED_VEHICLE_MODEL);
        }
    }

    @Test
    void getSeedChunks_containsOilChangeChunk_usedByIntegrationTest() {
        List<Chunk> chunks = SeedChunks.getSeedChunks();

        Chunk oilChunk = chunks.stream().filter(c -> "seed-chunk-1".equals(c.id())).findFirst().orElseThrow();
        assertThat(oilChunk.text()).contains("5000 miles");
        assertThat(oilChunk.section()).isEqualTo("Maintenance");
        assertThat(oilChunk.page()).isEqualTo(1);
    }

    @Test
    void getSeedChunks_returnsUnmodifiableList() {
        List<Chunk> chunks = SeedChunks.getSeedChunks();
        assertThat(chunks).isUnmodifiable();
    }
}
