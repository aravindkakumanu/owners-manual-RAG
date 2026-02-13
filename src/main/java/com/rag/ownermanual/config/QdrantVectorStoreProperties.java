package com.rag.ownermanual.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds and validates Qdrant vector store settings from application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "spring.ai.vectorstore.qdrant")
public class QdrantVectorStoreProperties {

    @NotBlank(message = "Qdrant host is required; set QDRANT_URL (or QDRANT_HOST) environment variable")
    private String host;

    private int port = 6334;

    /** Optional for local Qdrant; required when use-tls is true (Qdrant Cloud). */
    private String apiKey;

    private boolean useTls;

    /**
     * When using Qdrant Cloud (use-tls: true), API key is required. Without it you get 403 Forbidden at runtime.
     * This ensures fail-fast at startup with a clear message instead.
     */
    @AssertTrue(message = "Qdrant API key is required when use-tls is true (Qdrant Cloud); set QDRANT_API_KEY in .env")
    public boolean isApiKeyValidForTls() {
        return !useTls || (apiKey != null && !apiKey.isBlank());
    }

    private boolean initializeSchema;

    @NotBlank(message = "Qdrant collection name is required")
    private String collectionName;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public boolean isInitializeSchema() {
        return initializeSchema;
    }

    public void setInitializeSchema(boolean initializeSchema) {
        this.initializeSchema = initializeSchema;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
