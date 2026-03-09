package com.rag.ownermanual.exception;

/**
 * Signals that a requested resource (e.g. ingestion job) does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

