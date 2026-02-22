package com.rag.ownermanual.controller;

import com.rag.ownermanual.dto.common.ApiErrorResponse;
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
}
