package com.rag.ownermanual.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds and validates Groq chat/LLM settings from application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "spring.ai.openai.chat")
public class GroqChatProperties {

    @NotBlank(message = "Chat API key is required; set GROQ_API_KEY (or OPENAI_API_KEY) environment variable")
    private String apiKey;

    private String baseUrl;

    private Options options = new Options();

    public static class Options {
        @NotBlank(message = "Chat model is required; set GROQ_MODEL (e.g. llama-3.1-8b-instant) environment variable")
        private String model;
        private int maxTokens = 1024;
        private double temperature = 0.1;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }
}
