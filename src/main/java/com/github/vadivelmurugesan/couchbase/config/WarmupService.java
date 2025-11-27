package com.github.vadivelmurugesan.couchbase.config;

import com.github.vadivelmurugesan.couchbase.domain.DocumentQueryPort;
import com.github.vadivelmurugesan.couchbase.domain.model.DocumentCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Warm-up logic to prepare Couchbase & JVM after startup and after cluster restarts.
 */
@Service
public class WarmupService {

    private static final Logger log = LoggerFactory.getLogger(WarmupService.class);

    private final DocumentQueryPort port;

    public WarmupService(DocumentQueryPort port) {
        this.port = port;
    }

    public void performWarmup() {
        var criteria = new DocumentCriteria()
                .setType("user")
                .setMinAge(20)
                .setMaxAge(60);

        log.info("Starting Couchbase warm-up...");

        for (int i = 1; i <= 5; i++) {
            try {
                port.findByCriteria(criteria)
                        .take(1)
                        .switchIfEmpty(Mono.empty())
                        .blockLast();
                log.info("Warm-up iteration {} completed.", i);
            } catch (Exception e) {
                log.warn("Warm-up iteration {} failed: {}", i, e.getMessage());
            }
        }

        log.info("Warm-up completed.");
    }
}
