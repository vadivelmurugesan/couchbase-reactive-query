package com.github.vadivelmurugesan.couchbase.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Application-level timeout configuration.
 */
@ConfigurationProperties(prefix = "app.timeout")
public class AppTimeoutProperties {

    /**
     * Per-call reactive timeout for Couchbase queries.
     */
    private Duration query = Duration.ofSeconds(3);

    public Duration getQuery() {
        return query;
    }

    public void setQuery(Duration query) {
        this.query = query;
    }
}
