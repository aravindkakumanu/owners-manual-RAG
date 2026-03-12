package com.rag.ownermanual.service;

import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;
import com.rag.ownermanual.domain.ParsedPage;
import com.rag.ownermanual.dto.ingest.IngestRequest;
import com.rag.ownermanual.dto.ingest.IngestResponse;
import com.rag.ownermanual.repository.IngestionJobRepository;
import com.rag.ownermanual.repository.VectorStoreRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates the ingestion write path: create job → (async) parse → chunk → upsert.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final IngestionJobRepository ingestionJobRepository;
    private final RemoteDocumentParser documentParser;
    private final Chunker chunker;
    private final VectorStoreRepository vectorStoreRepository;

    private final MeterRegistry meterRegistry;

    public IngestionService(IngestionJobRepository ingestionJobRepository,
                            RemoteDocumentParser documentParser,
                            Chunker chunker,
                            VectorStoreRepository vectorStoreRepository,
                            MeterRegistry meterRegistry) {
        this.ingestionJobRepository = Objects.requireNonNull(ingestionJobRepository, "ingestionJobRepository");
        this.documentParser = Objects.requireNonNull(documentParser, "documentParser");
        this.chunker = Objects.requireNonNull(chunker, "chunker");
        this.vectorStoreRepository = Objects.requireNonNull(vectorStoreRepository, "vectorStoreRepository");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    /**
     * Start ingestion: create a job in CREATED state, persist it, run the pipeline, and return the job id.
     * @param request validated ingest request (manualId, documentUrl)
     * @return response containing the created job's id for status polling
     */
    public IngestResponse startIngestion(IngestRequest request) {
        Instant now = Instant.now();
        UUID jobId = UUID.randomUUID();
        IngestionJob job = new IngestionJob(
                jobId,
                request.manualId(),
                IngestionJobStatus.CREATED,
                null,
                now,
                now
        );
        ingestionJobRepository.save(job);
        log.info("Created ingestion job id={} manualId={}", jobId, request.manualId());

        Counter.builder("ingest.jobs.started")
                .description("Number of ingestion jobs started")
                .tags(Tags.of("vehicleModel", tagValue(request.manualId())))
                .register(meterRegistry)
                .increment();

        return new IngestResponse(jobId);
    }

    /**
     * Run the ingestion pipeline for a job: PROCESSING → parse → chunk → upsert → COMPLETED
     * (or FAILED on error). Only transitions from CREATED; otherwise no-op.
     * @param jobId       id of the job to process (must exist and be in CREATED state)
     * @param documentUrl URL of the document to fetch and parse
     */
    public void processJob(UUID jobId, String documentUrl) {
        var optJob = ingestionJobRepository.findById(jobId);
        if (optJob.isEmpty()) {
            log.warn("processJob: job not found id={}", jobId);
            return;
        }

        IngestionJob job = optJob.get();
        if (job.status() != IngestionJobStatus.CREATED) {
            log.debug("processJob: job id={} already in status {}; skipping", jobId, job.status());
            return;
        }

        ingestionJobRepository.updateStatus(jobId, IngestionJobStatus.PROCESSING, null);

        log.info("Processing ingestion job id={} manualId={}", jobId, job.manualId());

        Timer.Sample jobSample = Timer.start(meterRegistry);
        String vehicleModel = job.manualId();

        try {
            List<ParsedPage> pages = documentParser.fetchAndParse(documentUrl);
            // MVP: use manualId as vehicleModel for chunk metadata and query filtering.
            List<Chunk> chunks = chunker.chunk(pages, job.manualId(), vehicleModel);

            if (!chunks.isEmpty()) {
                vectorStoreRepository.upsertChunks(chunks);
                log.info("Upserted {} chunk(s) for job id={} manualId={}", chunks.size(), jobId, job.manualId());

                Counter.builder("ingest.chunks.processed")
                        .description("Total chunks processed during ingestion")
                        .tags(Tags.of("vehicleModel", tagValue(vehicleModel)))
                        .register(meterRegistry)
                        .increment(chunks.size());
            } else {
                log.info("No chunks produced for job id={} manualId={} (empty or blank pages)", jobId, job.manualId());
            }

            ingestionJobRepository.updateStatus(jobId, IngestionJobStatus.COMPLETED, null);
            log.info("Completed ingestion job id={} manualId={} chunkCount={}", jobId, job.manualId(), chunks.size());

            recordJobDuration(jobSample, "completed", vehicleModel, null);
        } catch (Exception e) {
            log.error("Ingestion failed for job id={} manualId={}", jobId, job.manualId(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            ingestionJobRepository.updateStatus(jobId, IngestionJobStatus.FAILED, errorMessage);

            recordJobDuration(jobSample, "failed", vehicleModel, e);
        }
    }

    /**
     * Asynchronously run the ingestion pipeline for a job using Spring's @Async annotation.
     * @param jobId       id of the job to process
     * @param documentUrl URL of the document to fetch and parse
     */
    @Async("ingestionTaskExecutor")
    public void processJobAsync(UUID jobId, String documentUrl) {
        processJob(jobId, documentUrl);
    }

    private void recordJobDuration(Timer.Sample sample, String status, String vehicleModel, Exception error) {
        Tags baseTags = Tags.of(
                "status", status,
                "vehicleModel", tagValue(vehicleModel)
        );

        if ("completed".equals(status)) {
            Counter.builder("ingest.jobs.completed")
                    .description("Number of ingestion jobs completed successfully")
                    .tags(baseTags)
                    .register(meterRegistry)
                    .increment();

            sample.stop(Timer.builder("ingest.jobs.duration")
                    .description("Duration of ingestion jobs from PROCESSING to terminal state")
                    .tags(baseTags)
                    .register(meterRegistry));
        } else {
            String errorType = error != null ? error.getClass().getSimpleName() : "Unknown";
            Tags failedTags = baseTags.and("errorType", errorType);

            Counter.builder("ingest.jobs.failed")
                    .description("Number of ingestion jobs that failed")
                    .tags(failedTags)
                    .register(meterRegistry)
                    .increment();

            sample.stop(Timer.builder("ingest.jobs.duration")
                    .description("Duration of ingestion jobs from PROCESSING to terminal state")
                    .tags(failedTags)
                    .register(meterRegistry));
        }
    }

    private static String tagValue(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value;
    }
}
