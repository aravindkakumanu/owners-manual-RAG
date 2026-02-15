package com.rag.ownermanual.dto.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rag.ownermanual.domain.IngestionJobStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IngestDtoTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    /** JavaTimeModule required so Instant (JobStatusResponse timestamps) serializes; Spring Boot app has this by default. */
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("IngestRequest validation")
    class IngestRequestValidation {

        @Test
        void validRequest_hasNoViolations() {
            var request = new IngestRequest("manual-123", "https://example.com/manual.pdf");
            Set<ConstraintViolation<IngestRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankManualId_failsValidation() {
            var request = new IngestRequest("   ", "https://example.com/manual.pdf");
            Set<ConstraintViolation<IngestRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> "manual_id is required".equals(v.getMessage()));
        }

        @Test
        void nullManualId_failsValidation() {
            var request = new IngestRequest(null, "https://example.com/manual.pdf");
            Set<ConstraintViolation<IngestRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isNotEmpty();
        }

        @Test
        void blankDocumentUrl_failsValidation() {
            var request = new IngestRequest("manual-123", "  ");
            Set<ConstraintViolation<IngestRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> "document URL is required".equals(v.getMessage()));
        }

        @Test
        void manualIdWithinMaxLength_passes() {
            var request = new IngestRequest("a".repeat(255), "https://example.com/doc.pdf");
            Set<ConstraintViolation<IngestRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        void manualIdExceedsMaxLength_failsValidation() {
            var request = new IngestRequest("a".repeat(256), "https://example.com/doc.pdf");
            Set<ConstraintViolation<IngestRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage() != null && v.getMessage().contains("255"));
        }

        @Test
        void documentUrlExceedsMaxLength_failsValidation() {
            var request = new IngestRequest("manual-1", "https://example.com/" + "a".repeat(2040));
            Set<ConstraintViolation<IngestRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage() != null && v.getMessage().contains("2048"));
        }
    }

    @Nested
    @DisplayName("IngestResponse and JobStatusResponse serialization")
    class ResponseSerialization {

        @Test
        void ingestResponse_serializesToJson() throws Exception {
            var jobId = UUID.randomUUID();
            var response = new IngestResponse(jobId);

            String json = MAPPER.writeValueAsString(response);

            assertThat(json).contains("jobId");
            assertThat(json).contains(jobId.toString());
        }

        @Test
        void ingestResponse_deserializesFromJson() throws Exception {
            var jobId = UUID.randomUUID();
            String json = "{\"jobId\":\"" + jobId + "\"}";

            IngestResponse response = MAPPER.readValue(json, IngestResponse.class);

            assertThat(response.jobId()).isEqualTo(jobId);
        }

        @Test
        void jobStatusResponse_full_serializesToJson() throws Exception {
            var jobId = UUID.randomUUID();
            var now = Instant.now();
            var response = new JobStatusResponse(
                    jobId,
                    IngestionJobStatus.PROCESSING,
                    null,
                    now,
                    now
            );

            String json = MAPPER.writeValueAsString(response);

            assertThat(json).contains("jobId");
            assertThat(json).contains(jobId.toString());
            assertThat(json).contains("PROCESSING");
            assertThat(json).contains("createdAt");
            assertThat(json).contains("updatedAt");
        }

        @Test
        void jobStatusResponse_failed_withErrorMessage_serializesToJson() throws Exception {
            var jobId = UUID.randomUUID();
            var response = new JobStatusResponse(
                    jobId,
                    IngestionJobStatus.FAILED,
                    "Document URL returned 404",
                    null,
                    null
            );

            String json = MAPPER.writeValueAsString(response);

            assertThat(json).contains("FAILED");
            assertThat(json).contains("Document URL returned 404");
        }
    }
}
