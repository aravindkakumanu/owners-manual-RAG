package com.rag.ownermanual.seed;

import com.rag.ownermanual.repository.VectorStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Loads seed chunks into the vector store.
*/
@Component
@Profile("seed")
public class SeedDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    private final VectorStoreRepository vectorStoreRepository;

    public SeedDataLoader(VectorStoreRepository vectorStoreRepository) {
        this.vectorStoreRepository = vectorStoreRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        var chunks = SeedChunks.getSeedChunks();
        vectorStoreRepository.upsertChunks(chunks);
        log.info("Seed profile active: upserted {} chunks (manual_id={}, vehicle_model={})",
                chunks.size(), SeedChunks.SEED_MANUAL_ID, SeedChunks.SEED_VEHICLE_MODEL);
    }
}
