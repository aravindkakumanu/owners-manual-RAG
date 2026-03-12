package com.rag.ownermanual.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CorrelationIdFilter: correlation ID generation, propagation from client,
 * MDC usage, and response header.
 */
class CorrelationIdFilterTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", Pattern.CASE_INSENSITIVE);

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("When client does not send X-Correlation-Id, filter generates UUID and sets header and MDC")
    void noHeader_generatesUuid_setsHeaderAndMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/query");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] mdcValueInChain = new String[1];

        FilterChain chain = (req, res) -> {
            mdcValueInChain[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
        };

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank().matches(UUID_PATTERN);
        assertThat(mdcValueInChain[0]).isEqualTo(header);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("When client sends X-Correlation-Id, filter uses it in response and MDC")
    void withHeader_echoesInResponseAndMdc() throws ServletException, IOException {
        String clientId = "client-provided-id-123";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/ingest");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, clientId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] mdcValueInChain = new String[1];

        FilterChain chain = (req, res) -> {
            mdcValueInChain[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(clientId);
        assertThat(mdcValueInChain[0]).isEqualTo(clientId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("When client sends blank X-Correlation-Id, filter generates UUID")
    void blankHeader_generatesUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/query");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {};

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank().matches(UUID_PATTERN);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("MDC is cleared after chain completes even when chain throws")
    void mdcClearedWhenChainThrows() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/query");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new RuntimeException("simulated failure");
        };

        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException e) {
            assertThat(e).hasMessage("simulated failure");
        }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Filter has highest precedence so it runs first")
    void orderIsHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }
}
