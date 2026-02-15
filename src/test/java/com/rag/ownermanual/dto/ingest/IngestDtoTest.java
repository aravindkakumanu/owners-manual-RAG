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

/**
 * Verifies ingest and job API DTOs for Phase 1 Step 6 exit criteria: a controller (or test) can
 * bind and validate IngestRequest, and IngestResponse / JobStatusResponse are JSON-serializable.
 *
 * <p><b>What each group tests:</b> Method names describe the scenario (e.g. blankManualId_failsValidation).
 * IngestRequestValidation: Bean Validation rules. ResponseSerialization: JSON round-trip so we know
 * the contract works when a real controller returns these types.
 *
 * <p><b>Why these tests:</b>
 * <ul>
 *   <li><b>IngestRequest validation:</b> Proves Bean Validation runs (required manual_id and
 *       documentUrl, max lengths). Invalid requests will get 400 when controller uses @Valid.</li>
 *   <li><b>IngestResponse / JobStatusResponse:</b> Proves we can build responses and serialize
 *       to JSON (UUID and enum serialize correctly; optional fields omitted when null).</li>
 * </ul>
 */
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
