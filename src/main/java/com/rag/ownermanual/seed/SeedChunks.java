package com.rag.ownermanual.seed;

import com.rag.ownermanual.domain.Chunk;

import java.util.List;

/**
 * Fixed seed chunks for dev/test: one manual, one vehicle_model, with distinct
 * queryable text so search and vehicle_model filter can be verified.
 */
public final class SeedChunks {

    /** manual_id for all seed chunks. */
    public static final String SEED_MANUAL_ID = "seed-manual-1";

    /** vehicle_model for all seed chunks. */
    public static final String SEED_VEHICLE_MODEL = "mvp-manual";

    private static final List<Chunk> SEED_CHUNKS = List.of(
            new Chunk(
                    "seed-chunk-1",
                    "Oil change interval is 5000 miles.",
                    SEED_MANUAL_ID,
                    SEED_VEHICLE_MODEL,
                    "Maintenance",
                    1
            ),
            new Chunk(
                    "seed-chunk-2",
                    "Check tire pressure monthly.",
                    SEED_MANUAL_ID,
                    SEED_VEHICLE_MODEL,
                    "Safety",
                    2
            ),
            new Chunk(
                    "seed-chunk-3",
                    "Coolant level should be between MIN and MAX.",
                    SEED_MANUAL_ID,
                    SEED_VEHICLE_MODEL,
                    "Maintenance",
                    3
            ),
            new Chunk(
                    "seed-chunk-4",
                    "Replace brake fluid every two years.",
                    SEED_MANUAL_ID,
                    SEED_VEHICLE_MODEL,
                    "Maintenance",
                    4
            ),
            new Chunk(
                    "seed-chunk-5",
                    "Battery terminals must be clean and tight.",
                    SEED_MANUAL_ID,
                    SEED_VEHICLE_MODEL,
                    "Electrical",
                    5
            )
    );

    private SeedChunks() {
        // utility; no instances
    }

    /**
     * Returns the fixed list of seed chunks. Unmodifiable; same list used by
     * seeding component and integration tests.
     */
    public static List<Chunk> getSeedChunks() {
        return SEED_CHUNKS;
    }
}
