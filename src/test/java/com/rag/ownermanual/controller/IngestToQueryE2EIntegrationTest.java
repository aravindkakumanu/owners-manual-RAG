package com.rag.ownermanual.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full end-to-end verification for ingest a manual via URL → poll job → query using ingested content.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "QDRANT_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
@Import(IngestToQueryE2EIntegrationTest.TestPdfController.class)
class IngestToQueryE2EIntegrationTest {

    private static final String MANUAL_ID = "e2e-manual-001";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final String TEST_API_KEY = "test-api-key";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Ingest → poll job → query returns answer and citations from ingested manual")
    void ingestThenQuery_fullLoop_succeeds() {
        String documentUrl = "http://localhost:" + port + "/test-data/manual.pdf";

        UUID jobId = startIngestion(MANUAL_ID, documentUrl);

        Map<String, Object> jobStatus = waitForTerminalJobState(jobId);
        assertThat(jobStatus.get("status")).isEqualTo("COMPLETED");

        // Query using a question that only the ingested manual can answer.
        String requestBody = """
                {"text": "What is the engine oil change interval?", "vehicleModel": "%s"}
                """.formatted(MANUAL_ID);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", TEST_API_KEY);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/query",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).as("response body").isNotNull();

        assertThat(body.get("answer")).as("answer").isNotNull();
        assertThat(body.get("answer").toString().trim()).as("answer non-empty").isNotEmpty();

        @SuppressWarnings("unchecked")
        List<Object> citations = (List<Object>) body.get("citations");
        assertThat(citations).as("citations list").isNotNull();
        assertThat(citations).as("at least one citation from ingested manual").isNotEmpty();
    }

    @Test
    @DisplayName("Ingest with unparseable PDF URL results in FAILED job without 500 to client")
    void ingestWithInvalidPdf_setsFailedJobStatus() {
        String documentUrl = "http://localhost:" + port + "/test-data/invalid.pdf";

        UUID jobId = startIngestion(MANUAL_ID, documentUrl);

        Map<String, Object> jobStatus = waitForTerminalJobState(jobId);
        assertThat(jobStatus.get("status")).isEqualTo("FAILED");
        assertThat(jobStatus.get("errorMessage")).as("error message").isNotNull();
    }

    private UUID startIngestion(String manualId, String documentUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", TEST_API_KEY);
        String body = """
                {"manualId":"%s","documentUrl":"%s"}
                """.formatted(manualId, documentUrl);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/ingest",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).as("ingest response body").isNotNull();
        assertThat(responseBody).containsKey("jobId");

        return UUID.fromString(responseBody.get("jobId").toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> waitForTerminalJobState(UUID jobId) {
        long deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();

        HttpHeaders jobPollHeaders = new HttpHeaders();
        jobPollHeaders.set("X-Api-Key", TEST_API_KEY);

        while (System.nanoTime() < deadline) {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "/api/v1/jobs/" + jobId,
                    HttpMethod.GET,
                    new HttpEntity<>(jobPollHeaders),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                assertThat(body).as("job status body").isNotNull();
                Object status = body.get("status");
                assertThat(status).as("job status").isNotNull();

                String statusString = status.toString();
                if ("COMPLETED".equals(statusString) || "FAILED".equals(statusString)) {
                    return body;
                }
            }

            try {
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for job status", e);
            }
        }

        throw new IllegalStateException("Timed out waiting for job " + jobId + " to reach a terminal state");
    }

    /**
     * Test-only controller that serves a tiny in-memory PDF so the ingestion
     * pipeline can fetch and parse it via HTTP without relying on external URLs.
     */
    @RestController
    static class TestPdfController {

        @GetMapping(path = "/test-data/manual.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public ResponseEntity<byte[]> manualPdf() throws IOException {
            byte[] pdfBytes = createSimplePdf("""
                    Owner Manual E2E Test

                    The engine oil change interval is 7000 miles.
                    """);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        }

        @GetMapping(path = "/test-data/invalid.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public ResponseEntity<byte[]> invalidPdf() {
            // Deliberately not a real PDF; Spring AI's PDF reader should fail to parse this.
            byte[] bytes = "not-a-real-pdf".getBytes();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(bytes.length);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        }

        /**
         * Creates a minimal single-page PDF containing the given text using
         * PDFBox (brought in transitively by spring-ai-pdf-document-reader).
         */
        private static byte[] createSimplePdf(String text) throws IOException {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    // Use a standard 14 font via PDFBox 3 API.
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 12);
                    contentStream.newLineAtOffset(50, 700);
                    for (String line : text.split("\\R")) {
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -16);
                    }
                    contentStream.endText();
                }

                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    document.save(out);
                    return out.toByteArray();
                }
            }
        }
    }
}

