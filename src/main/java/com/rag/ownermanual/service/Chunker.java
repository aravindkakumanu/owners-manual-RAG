package com.rag.ownermanual.service;

import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.domain.ParsedPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Turns parsed manual pages into Chunk instances suitable for storage in the vector store.
 * <ul>
 *   <li>Takes the parser output (ParsedPage list) plus manualId and vehicleModel.</li>
 *   <li>Splits each page's text into overlapping character-based chunks so embeddings stay within model limits.</li>
 *   <li>Assigns stable chunk identifiers so re-ingesting the same manual overwrites existing points in the vector store instead of duplicating them.</li>
 * </ul>
 */
@Component
public class Chunker {

    private static final Logger log = LoggerFactory.getLogger(Chunker.class);

    /**
     * Default maximum characters per chunk.
     */
    private static final int DEFAULT_MAX_CHARS = 2_000;

    /**
     * Default character overlap between consecutive chunks. Overlap reduces the chance
     * that an important sentence is cut exactly at a boundary and lost to retrieval.
     */
    private static final int DEFAULT_OVERLAP_CHARS = 200;

    private final int maxChars;
    private final int overlapChars;

    public Chunker() {
        this(DEFAULT_MAX_CHARS, DEFAULT_OVERLAP_CHARS);
    }

    Chunker(int maxChars, int overlapChars) {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        if (overlapChars < 0) {
            throw new IllegalArgumentException("overlapChars must be non-negative");
        }
        if (overlapChars >= maxChars) {
            throw new IllegalArgumentException("overlapChars must be smaller than maxChars");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    /**
     * Create chunks from parsed pages for a given manual and vehicle model.
     * @param pages        parsed pages from the document parser
     * @param manualId     identifier of the manual being ingested
     * @param vehicleModel vehicle/model this manual applies to
     * @return unmodifiable list of chunks, possibly empty but never null
     */
    public List<Chunk> chunk(List<ParsedPage> pages, String manualId, String vehicleModel) {
        if (pages == null || pages.isEmpty()) {
            return Collections.emptyList();
        }
        if (manualId == null || manualId.isBlank()) {
            throw new IllegalArgumentException("manualId must be non-blank");
        }
        if (vehicleModel == null || vehicleModel.isBlank()) {
            throw new IllegalArgumentException("vehicleModel must be non-blank");
        }

        List<Chunk> result = new ArrayList<>();

        int syntheticPageCounter = 0;
        for (ParsedPage page : pages) {
            if (page == null) {
                continue;
            }

            String text = page.text();
            if (text == null) {
                continue;
            }
            text = text.strip();
            if (text.isEmpty()) {
                continue;
            }

            Integer pageNumber = page.pageNumber();
            if (pageNumber == null) {
                syntheticPageCounter++;
            }
            int effectivePage = (pageNumber != null) ? pageNumber : syntheticPageCounter;

            createChunksForPage(result, text, page.section(), pageNumber, effectivePage, manualId, vehicleModel);
        }

        List<Chunk> unmodifiable = Collections.unmodifiableList(result);
        log.info("Chunked parsed pages into {} chunk(s) for manualId={} vehicleModel={}", unmodifiable.size(), manualId, vehicleModel);
        return unmodifiable;
    }

    private void createChunksForPage(List<Chunk> target,
                                     String pageText,
                                     String section,
                                     Integer pageNumber,
                                     int effectivePage,
                                     String manualId,
                                     String vehicleModel) {

        Objects.requireNonNull(pageText, "pageText");

        int length = pageText.length();
        if (length == 0) {
            return;
        }

        int chunkIndexWithinPage = 0;
        int start = 0;

        while (start < length) {
            int end = Math.min(start + maxChars, length);
            String slice = pageText.substring(start, end);

            if (slice.isBlank()) {
                break;
            }

            chunkIndexWithinPage++;
            String chunkId = buildChunkId(manualId, effectivePage, chunkIndexWithinPage);
            Chunk chunk = new Chunk(chunkId, slice, manualId, vehicleModel, section, pageNumber);
            target.add(chunk);

            if (end == length) {
                break;
            }

            start = end - overlapChars;
            if (start < 0) {
                start = 0;
            }
        }
    }

    private static String buildChunkId(String manualId, int pageNumber, int indexWithinPage) {
        return manualId + "-p" + pageNumber + "-" + indexWithinPage;
    }
}

