package com.rag.ownermanual.service;

import com.rag.ownermanual.domain.Chunk;
import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.domain.IngestionJobStatus;
import com.rag.ownermanual.domain.ParsedPage;
import com.rag.ownermanual.dto.ingest.IngestRequest;
import com.rag.ownermanual.dto.ingest.IngestResponse;
import com.rag.ownermanual.exception.DocumentProcessingException;
import com.rag.ownermanual.repository.IngestionJobRepository;
import com.rag.ownermanual.repository.VectorStoreRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    private static final String MANUAL_ID = "manual-123";
    private static final String DOCUMENT_URL = "https://example.com/manual.pdf";

    @Mock
    private IngestionJobRepository ingestionJobRepository;

    @Mock
    private RemoteDocumentParser documentParser;

    @Mock
    private Chunker chunker;

    @Mock
    private VectorStoreRepository vectorStoreRepository;

    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new IngestionService(
                ingestionJobRepository,
                documentParser,
                chunker,
                vectorStoreRepository,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void startIngestion_createsJobInCreatedState_andReturnsJobId() {
        IngestRequest request = new IngestRequest(MANUAL_ID, DOCUMENT_URL);
        ArgumentCaptor<IngestionJob> jobCaptor = ArgumentCaptor.forClass(IngestionJob.class);

        IngestResponse response = ingestionService.startIngestion(request);

        assertThat(response.jobId()).isNotNull();
        verify(ingestionJobRepository).save(jobCaptor.capture());
        IngestionJob saved = jobCaptor.getValue();
        assertThat(saved.id()).isEqualTo(response.jobId());
        assertThat(saved.manualId()).isEqualTo(MANUAL_ID);
        assertThat(saved.status()).isEqualTo(IngestionJobStatus.CREATED);
        assertThat(saved.errorMessage()).isNull();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.updatedAt()).isNotNull();
        verify(documentParser, never()).fetchAndParse(any());
    }

    @Test
    void processJob_whenJobExistsAndCreated_runsPipelineAndSetsCompleted() {
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(jobId, MANUAL_ID, IngestionJobStatus.CREATED, null, now, now);

        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        List<ParsedPage> pages = List.of(new ParsedPage(1, "Page one text", null));
        when(documentParser.fetchAndParse(DOCUMENT_URL)).thenReturn(pages);
        List<Chunk> chunks = List.of(
                new Chunk(MANUAL_ID + "-p1-1", "Page one text", MANUAL_ID, MANUAL_ID, null, 1)
        );
        when(chunker.chunk(pages, MANUAL_ID, MANUAL_ID)).thenReturn(chunks);

        ingestionService.processJob(jobId, DOCUMENT_URL);

        verify(ingestionJobRepository).updateStatus(jobId, IngestionJobStatus.PROCESSING, null);
        verify(documentParser).fetchAndParse(DOCUMENT_URL);
        verify(chunker).chunk(pages, MANUAL_ID, MANUAL_ID);
        verify(vectorStoreRepository).upsertChunks(chunks);
        verify(ingestionJobRepository).updateStatus(jobId, IngestionJobStatus.COMPLETED, null);
    }

    @Test
    void processJob_whenJobNotFound_doesNothing() {
        UUID jobId = UUID.randomUUID();
        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.empty());

        ingestionService.processJob(jobId, DOCUMENT_URL);

        verify(ingestionJobRepository, never()).updateStatus(any(), any(), any());
        verify(documentParser, never()).fetchAndParse(any());
        verify(vectorStoreRepository, never()).upsertChunks(any());
    }

    @Test
    void processJob_whenJobNotInCreatedState_skipsPipeline() {
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(jobId, MANUAL_ID, IngestionJobStatus.PROCESSING, null, now, now);

        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ingestionService.processJob(jobId, DOCUMENT_URL);

        verify(ingestionJobRepository, never()).updateStatus(eq(jobId), eq(IngestionJobStatus.PROCESSING), any());
        verify(documentParser, never()).fetchAndParse(any());
        verify(vectorStoreRepository, never()).upsertChunks(any());
    }

    @Test
    void processJob_whenParserThrows_setsFailedWithErrorMessage() {
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(jobId, MANUAL_ID, IngestionJobStatus.CREATED, null, now, now);
        String errorMessage = "Failed to fetch document from URL";

        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(documentParser.fetchAndParse(DOCUMENT_URL)).thenThrow(new DocumentProcessingException(errorMessage));

        ingestionService.processJob(jobId, DOCUMENT_URL);

        verify(ingestionJobRepository).updateStatus(jobId, IngestionJobStatus.PROCESSING, null);
        verify(documentParser).fetchAndParse(DOCUMENT_URL);
        verify(chunker, never()).chunk(any(), any(), any());
        verify(vectorStoreRepository, never()).upsertChunks(any());
        verify(ingestionJobRepository).updateStatus(jobId, IngestionJobStatus.FAILED, errorMessage);
    }

    @Test
    void processJob_whenChunksEmpty_doesNotUpsert_stillSetsCompleted() {
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(jobId, MANUAL_ID, IngestionJobStatus.CREATED, null, now, now);

        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(documentParser.fetchAndParse(DOCUMENT_URL)).thenReturn(List.of());
        when(chunker.chunk(List.of(), MANUAL_ID, MANUAL_ID)).thenReturn(List.of());

        ingestionService.processJob(jobId, DOCUMENT_URL);

        verify(ingestionJobRepository).updateStatus(jobId, IngestionJobStatus.PROCESSING, null);
        verify(documentParser).fetchAndParse(DOCUMENT_URL);
        verify(chunker).chunk(List.of(), MANUAL_ID, MANUAL_ID);
        verify(vectorStoreRepository, never()).upsertChunks(any());
        verify(ingestionJobRepository).updateStatus(jobId, IngestionJobStatus.COMPLETED, null);
    }

    @Test
    void processJobAsync_delegatesToProcessJobPipeline() {
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionJob job = new IngestionJob(jobId, MANUAL_ID, IngestionJobStatus.CREATED, null, now, now);

        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        List<ParsedPage> pages = List.of(new ParsedPage(1, "Async page text", null));
        when(documentParser.fetchAndParse(DOCUMENT_URL)).thenReturn(pages);
        List<Chunk> chunks = List.of(
                new Chunk(MANUAL_ID + "-p1-1", "Async page text", MANUAL_ID, MANUAL_ID, null, 1)
        );
        when(chunker.chunk(pages, MANUAL_ID, MANUAL_ID)).thenReturn(chunks);

        ingestionService.processJobAsync(jobId, DOCUMENT_URL);

        verify(ingestionJobRepository).updateStatus(jobId, IngestionJobStatus.PROCESSING, null);
        verify(documentParser).fetchAndParse(DOCUMENT_URL);
        verify(chunker).chunk(pages, MANUAL_ID, MANUAL_ID);
        verify(vectorStoreRepository).upsertChunks(chunks);
        verify(ingestionJobRepository).updateStatus(jobId, IngestionJobStatus.COMPLETED, null);
    }
}
