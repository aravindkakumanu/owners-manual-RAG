package com.rag.ownermanual.controller;

import com.rag.ownermanual.dto.query.QueryRequest;
import com.rag.ownermanual.dto.query.QueryResponse;
import com.rag.ownermanual.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter for the RAG query flow: POST /api/v1/query.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Query", description = "RAG query: submit a question, get answer with citations.")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Submit a question; returns a generated answer and citations from the manual.
     *
     * <p>Invalid input (missing text, text too long, etc.) yields 400 with a structured
     * error body (status, message, optional field errors).
     */
    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Query the RAG", description = "Submit a question; returns a generated answer and citations from the manual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success; answer and citations (citations may be empty list).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = QueryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g. blank text, text exceeds 8192 chars).",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public QueryResponse query(@Valid @RequestBody QueryRequest request) {
        return queryService.query(request.text(), request.vehicleModel());
    }
}
