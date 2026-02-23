package com.rag.ownermanual.controller;

import com.rag.ownermanual.dto.common.ApiErrorResponse;
import com.rag.ownermanual.exception.DownstreamLlmException;
import com.rag.ownermanual.exception.DownstreamVectorStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the API layer: maps exceptions to HTTP status and a structured error body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    private static final String SEARCH_UNAVAILABLE_MESSAGE = "Search is temporarily unavailable. Please try again later.";
    private static final String ANSWER_UNAVAILABLE_MESSAGE = "Answer could not be generated. Please try again later.";

    /**
     * Handles Bean Validation failures when the request body fails.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse.FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldErrorDetail(fe.getField(), fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
                .collect(Collectors.toList());

        log.warn("Request validation failed: {} field error(s). Errors: {}", errors.size(), errors);

        ApiErrorResponse body = ApiErrorResponse.validationError(
                HttpStatus.BAD_REQUEST.value(),
                VALIDATION_FAILED_MESSAGE,
                null,
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles failures talking to the vector store (Qdrant / Spring AI VectorStore).
     */
    @ExceptionHandler(DownstreamVectorStoreException.class)
    public ResponseEntity<ApiErrorResponse> handleVectorStoreFailure(DownstreamVectorStoreException ex) {
        log.error("Vector store failure while processing query", ex);

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                SEARCH_UNAVAILABLE_MESSAGE,
                null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * Handles failures calling the LLM (ChatClient / ChatModel).
     */
    @ExceptionHandler(DownstreamLlmException.class)
    public ResponseEntity<ApiErrorResponse> handleLlmFailure(DownstreamLlmException ex) {
        log.error("LLM failure while generating answer", ex);

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                ANSWER_UNAVAILABLE_MESSAGE,
                null
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
