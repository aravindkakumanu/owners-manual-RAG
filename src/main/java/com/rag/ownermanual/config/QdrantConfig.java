package com.rag.ownermanual.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Qdrant vector store configuration and validation.
 * The actual VectorStore bean is created by Spring AI auto-configuration from
 * spring.ai.vectorstore.qdrant.*; this config ensures those properties are
 * validated at startup (fail-fast when required env vars are missing).
 */
@Configuration
@EnableConfigurationProperties(QdrantVectorStoreProperties.class)
public class QdrantConfig {
}
