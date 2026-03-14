package com.rag.ownermanual.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Simple API-key based authentication filter.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_CLIENT_ROLE = "ROLE_API_CLIENT";

    private final String expectedApiKey;
    private final RequestMatcher protectedEndpointsMatcher;

    public ApiKeyAuthenticationFilter(@Value("${app.security.api-key}") String expectedApiKey) {
        this.expectedApiKey = Objects.requireNonNull(expectedApiKey, "app.security.api-key must be configured");
        this.protectedEndpointsMatcher = PathPatternRequestMatcher.withDefaults().matcher("/api/v1/**");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!protectedEndpointsMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        if (!constantTimeEquals(apiKeyHeader, expectedApiKey)) {
            log.warn("Invalid API key presented for path {}", request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        AbstractAuthenticationToken authentication = new AbstractAuthenticationToken(
                List.of(new SimpleGrantedAuthority(API_CLIENT_ROLE))) {

            @Override
            public Object getCredentials() {
                return "";
            }

            @Override
            public Object getPrincipal() {
                return "apiKey:default";
            }
        };
        authentication.setAuthenticated(true);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}

