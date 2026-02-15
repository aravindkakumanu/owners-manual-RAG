package com.rag.ownermanual.domain;

/**
 * Lifecycle status of an async ingestion job.
 */
public enum IngestionJobStatus {
    CREATED,
    PROCESSING,
    COMPLETED,
    FAILED
}
