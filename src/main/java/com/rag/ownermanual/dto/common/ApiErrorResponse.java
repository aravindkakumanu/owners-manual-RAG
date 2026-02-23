package com.rag.ownermanual.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Structured error body returned for 4xx/5xx responses. No stack traces or internal details.
 *
 * @param status     HTTP status code (e.g. 400, 503).
 * @param message    Human-readable message safe to show to the client.
 * @param traceId    Optional request/trace id for correlating with server logs.
 * @param fieldErrors Optional list of validation errors; present for 400, null for 5xx.
 */
@Schema(description = "Structured error response; no stack traces or internal details.")
public record ApiErrorResponse(
        @Schema(description = "HTTP status code", example = "400", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
        @Schema(description = "Human-readable message safe to show to the client", requiredMode = Schema.RequiredMode.REQUIRED)
        String message,
        @Schema(description = "Optional trace id for correlating with server logs")
        String traceId,
        @Schema(description = "Field-level validation errors; present for 400, null for 5xx")
        List<FieldErrorDetail> fieldErrors
) {
    /**
     * Single field validation error (field name + constraint message).
     *
     * @param field   Name of the request field that failed validation (e.g. "text", "vehicleModel").
     * @param message Constraint message from the validator (e.g. "query text is required").
     */
    public record FieldErrorDetail(
            @Schema(description = "Request field that failed validation")
            String field,
            @Schema(description = "Constraint message from the validator")
            String message
    ) {}

    /**
     * Builds a 400 response with field errors (validation failure).
     */
    public static ApiErrorResponse validationError(int status, String message, String traceId, List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(status, message, traceId, fieldErrors != null ? fieldErrors : List.of());
    }

    /**
     * Builds an error response without field errors (e.g. 503 for downstream failure).
     * Use for vector/LLM failures so we don't expose internal details.
     */
    public static ApiErrorResponse of(int status, String message, String traceId) {
        return new ApiErrorResponse(status, message, traceId, null);
    }
}
