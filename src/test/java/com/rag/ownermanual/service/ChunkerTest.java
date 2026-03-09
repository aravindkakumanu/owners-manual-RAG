package com.rag.ownermanual.service;

import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.domain.ParsedPage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Chunker: verifies chunk boundaries, metadata propagation, and id stability for re-ingestion.
 */
class ChunkerTest {

    private static final String MANUAL_ID = "manual-123";
    private static final String VEHICLE_MODEL = "Model-X";

    @Test
    void chunk_returnsEmptyList_whenPagesNullOrEmpty() {
        Chunker chunker = new Chunker(50, 10);

        assertThat(chunker.chunk(null, MANUAL_ID, VEHICLE_MODEL)).isEmpty();
        assertThat(chunker.chunk(List.of(), MANUAL_ID, VEHICLE_MODEL)).isEmpty();
    }

    @Test
    void chunk_throwsForBlankManualOrVehicleModel() {
        Chunker chunker = new Chunker(50, 10);
        List<ParsedPage> pages = List.of(new ParsedPage(1, "Some text", null));

        assertThatThrownBy(() -> chunker.chunk(pages, " ", VEHICLE_MODEL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manualId");

        assertThatThrownBy(() -> chunker.chunk(pages, MANUAL_ID, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vehicleModel");
    }

    @Test
    void chunk_createsSingleChunk_whenTextFitsWithinLimit() {
        Chunker chunker = new Chunker(50, 10);
        String text = "Short manual page content.";
        ParsedPage page = new ParsedPage(3, text, "Engine");

        List<Chunk> chunks = chunker.chunk(List.of(page), MANUAL_ID, VEHICLE_MODEL);

        assertThat(chunks).hasSize(1);
        Chunk chunk = chunks.getFirst();
        assertThat(chunk.id()).isEqualTo(MANUAL_ID + "-p3-1");
        assertThat(chunk.text()).isEqualTo(text);
        assertThat(chunk.manualId()).isEqualTo(MANUAL_ID);
        assertThat(chunk.vehicleModel()).isEqualTo(VEHICLE_MODEL);
        assertThat(chunk.section()).isEqualTo("Engine");
        assertThat(chunk.page()).isEqualTo(3);
    }

    @Test
    void chunk_splitsLongPageIntoOverlappingChunks_withStableIds() {
        int maxChars = 20;
        int overlap = 5;
        Chunker chunker = new Chunker(maxChars, overlap);

        String base = "abcdefghijklmnopqrstuvwxyz"; // 26 chars
        ParsedPage page = new ParsedPage(1, base, null);

        List<Chunk> chunks = chunker.chunk(List.of(page), MANUAL_ID, VEHICLE_MODEL);

        assertThat(chunks).hasSize(2);

        Chunk first = chunks.get(0);
        Chunk second = chunks.get(1);

        assertThat(first.id()).isEqualTo(MANUAL_ID + "-p1-1");
        assertThat(second.id()).isEqualTo(MANUAL_ID + "-p1-2");

        assertThat(first.text()).isEqualTo(base.substring(0, 20));
        assertThat(second.text()).isEqualTo(base.substring(15)); // 20 - overlap
    }

    @Test
    void chunk_usesSyntheticPageIndex_whenPageNumberMissing_butKeepsNullPageOnChunk() {
        Chunker chunker = new Chunker(50, 10);
        ParsedPage first = new ParsedPage(null, "First page text", null);
        ParsedPage second = new ParsedPage(null, "Second page text", null);

        List<Chunk> chunks = chunker.chunk(List.of(first, second), MANUAL_ID, VEHICLE_MODEL);

        assertThat(chunks).hasSize(2);

        Chunk c1 = chunks.get(0);
        Chunk c2 = chunks.get(1);

        // Synthetic page indices are 1 and 2, but the public page field stays null.
        assertThat(c1.id()).isEqualTo(MANUAL_ID + "-p1-1");
        assertThat(c1.page()).isNull();

        assertThat(c2.id()).isEqualTo(MANUAL_ID + "-p2-1");
        assertThat(c2.page()).isNull();
    }

    @Test
    void chunk_isDeterministic_givenSameInputs() {
        Chunker chunker = new Chunker(20, 5);
        ParsedPage page = new ParsedPage(2, "Deterministic page content for testing.", "Section");
        List<ParsedPage> pages = List.of(page);

        List<Chunk> first = chunker.chunk(pages, MANUAL_ID, VEHICLE_MODEL);
        List<Chunk> second = chunker.chunk(pages, MANUAL_ID, VEHICLE_MODEL);

        assertThat(first).hasSameSizeAs(second);
        for (int i = 0; i < first.size(); i++) {
            Chunk c1 = first.get(i);
            Chunk c2 = second.get(i);
            assertThat(c1.id()).isEqualTo(c2.id());
            assertThat(c1.text()).isEqualTo(c2.text());
            assertThat(c1.manualId()).isEqualTo(c2.manualId());
            assertThat(c1.vehicleModel()).isEqualTo(c2.vehicleModel());
            assertThat(c1.page()).isEqualTo(c2.page());
            assertThat(c1.section()).isEqualTo(c2.section());
        }
    }
}

