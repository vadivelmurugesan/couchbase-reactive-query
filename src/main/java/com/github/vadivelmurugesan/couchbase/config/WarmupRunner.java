package com.github.vadivelmurugesan.couchbase.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Triggers warm-up once at application startup.
 */
@Component
public class WarmupRunner {

    private final WarmupService warmupService;

    public WarmupRunner(WarmupService warmupService) {
        this.warmupService = warmupService;
    }

    @PostConstruct
    public void startupWarmup() {
        warmupService.performWarmup();
    }
}
