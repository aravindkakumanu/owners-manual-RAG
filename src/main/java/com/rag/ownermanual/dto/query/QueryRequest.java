package com.rag.ownermanual.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/query: user question and optional filters/image.
 * @param text         User question (required). Max length 8192 characters.
 * @param vehicleModel Optional vehicle/model to scope the search; applied as filter in vector search.
 * @param imageUrl     Optional URL of an image (e.g. for future vision/multimodal queries).
 * @param imageBase64  Optional base64-encoded image data (alternative to imageUrl; one of imageUrl or imageBase64).
 */
@Schema(description = "Request body for POST /api/v1/query: user question and optional filters/image.")
public record QueryRequest(
        @NotBlank(message = "query text is required")
        @Size(max = 8192, message = "query text must not exceed 8192 characters")
        @Schema(description = "User question (required)", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 8192)
        String text,

        @Size(max = 255)
        @Schema(description = "Optional vehicle/model to scope the search; applied as filter in vector search", maxLength = 255)
        String vehicleModel,

        @Size(max = 2048)
        @Schema(description = "Optional URL of an image (e.g. for future vision/multimodal queries)", maxLength = 2048)
        String imageUrl,

        @Schema(description = "Optional base64-encoded image data (alternative to imageUrl)")
        String imageBase64
) {}
