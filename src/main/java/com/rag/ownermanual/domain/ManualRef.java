package com.rag.ownermanual.domain;

/**
 * Lightweight reference to a manual 
 * 
 * @param id          Manual identifier (manual_id in API and storage; e.g. UUID or slug).
 * @param externalId  Optional correlation ID for an external system; null if not used.
 * @param vehicleModel Vehicle/model identifier; used for vector search filtering.
 */
public record ManualRef(
        String id,
        String externalId,
        String vehicleModel
) {
    public ManualRef {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id (manual_id) must be non-blank");
        }
        if (vehicleModel == null || vehicleModel.isBlank()) {
            throw new IllegalArgumentException("vehicleModel must be non-blank");
        }
    }
}
