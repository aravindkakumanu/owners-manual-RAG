package com.rag.ownermanual.controller;

import com.rag.ownermanual.domain.IngestionJob;
import com.rag.ownermanual.dto.ingest.IngestRequest;
import com.rag.ownermanual.dto.ingest.IngestResponse;
import com.rag.ownermanual.dto.ingest.JobStatusResponse;
import com.rag.ownermanual.exception.ResourceNotFoundException;
import com.rag.ownermanual.repository.IngestionJobRepository;
import com.rag.ownermanual.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * HTTP adapter for the ingestion pipeline: POST /api/v1/ingest and GET /api/v1/jobs/{id}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Ingest", description = "Ingest manuals via document URL and poll job status.")
public class IngestController {

    private final IngestionService ingestionService;
    private final IngestionJobRepository ingestionJobRepository;

    public IngestController(IngestionService ingestionService, IngestionJobRepository ingestionJobRepository) {
        this.ingestionService = ingestionService;
        this.ingestionJobRepository = ingestionJobRepository;
    }

    /**
     * Submit a manual_id and document URL; returns a created ingestion job id.
     */
    @PostMapping(
            value = "/ingest",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Ingest a document", description = "Submit a manual_id and document URL; returns job_id. Poll GET /jobs/{id} for status.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Ingest job created; use jobId to poll status.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = IngestResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error (e.g. blank manual_id or documentUrl).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public IngestResponse ingest(@Valid @RequestBody IngestRequest request) {
        IngestResponse response = ingestionService.startIngestion(request);
        ingestionService.processJobAsync(response.jobId(), request.documentUrl());
        return response;
    }

    /**
     * Get the current status of an ingestion job by id.
     */
    @GetMapping(
            value = "/jobs/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get job status", description = "Returns current status of an ingestion job (CREATED, PROCESSING, COMPLETED, FAILED).")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Job status and optional error_message, created_at, updated_at.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = JobStatusResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    public JobStatusResponse getJobStatus(@PathVariable("id") UUID jobId) {
        IngestionJob job = ingestionJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        return new JobStatusResponse(
                job.id(),
                job.status(),
                job.errorMessage(),
                job.createdAt(),
                job.updatedAt()
        );
    }
}

