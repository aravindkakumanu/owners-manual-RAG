package com.rag.ownermanual.dto.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/query: user question and optional filters/image.
 * @param text         User question (required). Max length 8192 characters.
 * @param vehicleModel Optional vehicle/model to scope the search; applied as filter in vector search.
 * @param imageUrl     Optional URL of an image (e.g. for future vision/multimodal queries).
 * @param imageBase64  Optional base64-encoded image data (alternative to imageUrl; one of imageUrl or imageBase64).
 */
public record QueryRequest(
        @NotBlank(message = "query text is required")
        @Size(max = 8192, message = "query text must not exceed 8192 characters")
        String text,

        @Size(max = 255)
        String vehicleModel,

        @Size(max = 2048)
        String imageUrl,

        String imageBase64
) {}
