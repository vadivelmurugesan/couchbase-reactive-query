package com.github.vadivelmurugesan.couchbase.adapter.couchbase;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Centralized helper for Couchbase query-related metrics.
 *
 * <p>This component avoids repeated metric name strings, enforces
 * proper Micrometer counter usage, and provides a safe extension
 * point for additional query metrics in the future.</p>
 *
 * </p>
 */
@Component
public final class QueryMetrics {

    /** Base metric prefix for all Couchbase query metrics. */
    private static final String PREFIX = "couchbase.query";

    /** Metric ID for prepared statement fallback. */
    private static final String METRIC_PREPARED_FALLBACK = PREFIX + ".prepared_fallback.count";

    private final Counter preparedFallbackCounter;

    public QueryMetrics(MeterRegistry registry) {
        Objects.requireNonNull(registry, "MeterRegistry must not be null");

        // Pre-register counters for better performance and consistency.
        this.preparedFallbackCounter = Counter.builder(METRIC_PREPARED_FALLBACK)
                .description("Number of times a prepared Couchbase query had to fallback to adhoc execution")
                .register(registry);
    }

    /** Increments the counter for prepared query fallback scenarios. */
    public void incrementPreparedFallback() {
        preparedFallbackCounter.increment();
    }
}
