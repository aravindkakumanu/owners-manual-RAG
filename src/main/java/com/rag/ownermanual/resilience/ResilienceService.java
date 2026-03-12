package com.rag.ownermanual.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Small wrapper so call sites can apply Resilience4j policies explicitly.
 */
@Component
public class ResilienceService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public ResilienceService(CircuitBreakerRegistry circuitBreakerRegistry,
                             RetryRegistry retryRegistry,
                             TimeLimiterRegistry timeLimiterRegistry) {
        this.circuitBreakerRegistry = Objects.requireNonNull(circuitBreakerRegistry, "circuitBreakerRegistry");
        this.retryRegistry = Objects.requireNonNull(retryRegistry, "retryRegistry");
        Objects.requireNonNull(timeLimiterRegistry, "timeLimiterRegistry");
    }

    /**
     * Execute a blocking supplier with retry + circuit breaker.
     */
    public <T> T execute(String name, Supplier<T> supplier) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(supplier, "supplier");

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);

        Supplier<T> decorated = CircuitBreaker.decorateSupplier(cb, supplier);
        decorated = Retry.decorateSupplier(retry, decorated);
        return decorated.get();
    }

    /**
     * Execute a blocking supplier with retry + circuit breaker + time limit.
     */
    public <T> T executeWithTimeLimit(String name, Supplier<T> supplier) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(supplier, "supplier");

        return execute(name, supplier);
    }
}

