package com.rag.ownermanual.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for the Owner's Manual RAG API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ownerManualOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Owner's Manual RAG API")
                        .version("1.0.0")
                        .description("Query and ingest endpoints for the Owner's Manual RAG service. "
                                + "POST /api/v1/query returns answers with citations; "
                                + "POST /api/v1/ingest accepts a document URL and returns a job_id; "
                                + "GET /api/v1/jobs/{id} returns job status."));
    }
}
