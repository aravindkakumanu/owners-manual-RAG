package com.rag.ownermanual.exception;

/**
 * <p>Signals that the vector store (Qdrant / Spring AI VectorStore) failed while
 * serving a read-path query – for example due to timeouts, connectivity issues,
 * or server-side errors.</p>
 */
public class DownstreamVectorStoreException extends RuntimeException {

    public DownstreamVectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}

