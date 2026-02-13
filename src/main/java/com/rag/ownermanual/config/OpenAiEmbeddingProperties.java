package com.rag.ownermanual.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds and validates OpenAI embedding settings from application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "spring.ai.openai.embedding")
public class OpenAiEmbeddingProperties {

    @NotBlank(message = "OpenAI API key is required for embeddings; set OPENAI_API_KEY environment variable")
    private String apiKey;

    private Options options = new Options();

    public static class Options {
        private String model = "text-embedding-3-small";
        @Positive
        private Integer dimensions = 1536;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Integer getDimensions() {
            return dimensions;
        }

        public void setDimensions(Integer dimensions) {
            this.dimensions = dimensions;
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }
}
