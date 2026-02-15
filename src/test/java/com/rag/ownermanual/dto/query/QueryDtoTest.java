package com.rag.ownermanual.dto.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryDtoTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("QueryRequest validation")
    class QueryRequestValidation {

        @Test
        void validRequest_hasNoViolations() {
            var request = new QueryRequest("How do I check the oil?", null, null, null);
            Set<ConstraintViolation<QueryRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankText_failsValidation() {
            var request = new QueryRequest("   ", null, null, null);
            Set<ConstraintViolation<QueryRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> "query text is required".equals(v.getMessage()));
        }

        @Test
        void nullText_failsValidation() {
            var request = new QueryRequest(null, null, null, null);
            Set<ConstraintViolation<QueryRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isNotEmpty();
        }

        @Test
        void textWithinMaxLength_passes() {
            var request = new QueryRequest("a".repeat(8192), null, null, null);
            Set<ConstraintViolation<QueryRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isEmpty();
        }

        @Test
        void textExceedsMaxLength_failsValidation() {
            var request = new QueryRequest("a".repeat(8193), null, null, null);
            Set<ConstraintViolation<QueryRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage() != null && v.getMessage().contains("8192"));
        }

        @Test
        void optionalVehicleModel_andImageFields_accepted() {
            var request = new QueryRequest("Question?", "Model-X", "https://example.com/img.png", null);
            Set<ConstraintViolation<QueryRequest>> violations = VALIDATOR.validate(request);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("QueryResponse and Citation")
    class QueryResponseAndCitation {

        @Test
        void responseWithCitations_serializesToJson() throws Exception {
            var citation = new Citation("chunk-1", "Engine", "Check oil level...", 5);
            var response = new QueryResponse("Use the dipstick.", List.of(citation));

            String json = MAPPER.writeValueAsString(response);

            assertThat(json).contains("answer");
            assertThat(json).contains("Use the dipstick.");
            assertThat(json).contains("citations");
            assertThat(json).contains("chunk-1");
            assertThat(json).contains("Engine");
            assertThat(json).contains("snippet");
        }

        @Test
        void responseWithEmptyCitations_serializesToJson() throws Exception {
            var response = new QueryResponse("No chunks found.", List.of());

            String json = MAPPER.writeValueAsString(response);

            assertThat(json).contains("No chunks found.");
            assertThat(json).contains("citations");
            assertThat(json).contains("[]");
        }

        @Test
        void of_withNullCitations_usesEmptyList() {
            var response = QueryResponse.of("Answer", null);

            assertThat(response.answer()).isEqualTo("Answer");
            assertThat(response.citations()).isEmpty();
        }

        @Test
        void constructor_withNullCitations_throws() {
            assertThatThrownBy(() -> new QueryResponse("Answer", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("citations must not be null");
        }

        @Test
        void citation_withBlankChunkId_throws() {
            assertThatThrownBy(() -> new Citation("  ", "Section", "snippet", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chunkId");
        }
    }
}
