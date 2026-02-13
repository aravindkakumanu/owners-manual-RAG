package com.rag.ownermanual.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables chat/LLM configuration and validation.
 * The ChatModel bean is created by Spring AI auto-configuration from
 * spring.ai.openai.chat.*; this config validates api-key and model
 * at startup (fail-fast when GROQ_API_KEY or GROQ_MODEL is missing).
 */
@Configuration
@EnableConfigurationProperties(GroqChatProperties.class)
public class ChatConfig {
}
