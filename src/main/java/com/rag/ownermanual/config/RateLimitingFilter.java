package com.rag.ownermanual.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-identity rate limiting filter for protected RAG endpoints.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RequestMatcher protectedEndpointsMatcher =
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/**");

    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    private final int capacity;
    private final int refillTokens;
    private final long refillPeriodSeconds;

    public RateLimitingFilter(
            @Value("${app.security.rate-limiting.capacity}") int capacity,
            @Value("${app.security.rate-limiting.refill-tokens}") int refillTokens,
            @Value("${app.security.rate-limiting.refill-period-seconds}") long refillPeriodSeconds
    ) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriodSeconds = refillPeriodSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!protectedEndpointsMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String identity = String.valueOf(authentication.getPrincipal());
        RateWindow window = windows.computeIfAbsent(identity, k -> new RateWindow(capacity, refillTokens, refillPeriodSeconds));
        if (!window.tryConsume()) {
            log.warn("Rate limit exceeded for identity={} path={}", identity, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static final class RateWindow {
        private final int capacity;
        private final int refillTokens;
        private final long refillPeriodSeconds;

        private final AtomicInteger tokens;
        private volatile long windowStartEpochSeconds;

        private RateWindow(int capacity, int refillTokens, long refillPeriodSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodSeconds = refillPeriodSeconds;
            this.tokens = new AtomicInteger(capacity);
            this.windowStartEpochSeconds = currentEpochSeconds();
        }

        boolean tryConsume() {
            long now = currentEpochSeconds();
            long elapsed = now - windowStartEpochSeconds;
            if (elapsed >= refillPeriodSeconds) {
                synchronized (this) {
                    if (now - windowStartEpochSeconds >= refillPeriodSeconds) {
                        int newTokens = Math.min(capacity, tokens.get() + refillTokens);
                        tokens.set(newTokens);
                        windowStartEpochSeconds = now;
                    }
                }
            }
            while (true) {
                int current = tokens.get();
                if (current <= 0) {
                    return false;
                }
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
            }
        }

        private long currentEpochSeconds() {
            return System.currentTimeMillis() / 1000L;
        }
    }
}

