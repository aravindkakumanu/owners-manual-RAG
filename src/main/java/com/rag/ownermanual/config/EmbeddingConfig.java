package com.rag.ownermanual.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables OpenAI embedding configuration and validation.
 * The EmbeddingModel bean is created by Spring AI auto-configuration from
 * spring.ai.openai.*; this config validates embedding api-key and options
 * at startup (fail-fast when OPENAI_API_KEY is missing).
 */
@Configuration
@EnableConfigurationProperties(OpenAiEmbeddingProperties.class)
public class EmbeddingConfig {
}
