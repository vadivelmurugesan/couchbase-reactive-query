package com.github.vadivelmurugesan.couchbase.config;

import com.couchbase.client.core.diagnostics.PingResult;
import com.couchbase.client.core.diagnostics.PingState;
import com.couchbase.client.java.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Detects Couchbase restarts/unhealthy states and triggers warm-up once recovered.
 */
@Component
public class CouchbaseMonitor {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseMonitor.class);

    private final Cluster cluster;
    private final WarmupService warmupService;

    private volatile boolean clusterWasDown = false;

    public CouchbaseMonitor(Cluster cluster, WarmupService warmupService) {
        this.cluster = cluster;
        this.warmupService = warmupService;
    }

    @Scheduled(fixedDelay = 5000)
    public void monitor() {
        try {
            PingResult result = cluster.ping();

            boolean anyFailure = result.endpoints().values().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(r -> r.state() != PingState.OK);

            if (anyFailure) {
                log.warn("Couchbase ping shows unhealthy state.");
                clusterWasDown = true;
                return;
            }

            if (clusterWasDown) {
                log.info("Couchbase cluster recovered. Triggering warm-up...");
                warmupService.performWarmup();
            }

            clusterWasDown = false;

        } catch (Exception ex) {
            log.error("Unable to ping Couchbase: {}", ex.getMessage());
            clusterWasDown = true;
        }
    }
}
