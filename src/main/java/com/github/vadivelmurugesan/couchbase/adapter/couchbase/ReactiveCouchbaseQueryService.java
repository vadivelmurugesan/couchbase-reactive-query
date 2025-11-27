package com.github.vadivelmurugesan.couchbase.adapter.couchbase;

import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.error.PlanningFailureException;
import com.couchbase.client.core.error.PreparedStatementFailureException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ReactiveCluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryScanConsistency;
import com.couchbase.client.java.query.ReactiveQueryResult;
import com.github.vadivelmurugesan.couchbase.config.AppTimeoutProperties;
import com.github.vadivelmurugesan.couchbase.domain.DocumentQueryPort;
import com.github.vadivelmurugesan.couchbase.domain.model.DocumentCriteria;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Clean + minimal Couchbase reactive query service.
 * Prepared statement → Adhoc fallback + retry.
 * CircuitBreaker & Bulkhead via annotations only.
 */
@Service
public final class ReactiveCouchbaseQueryService implements DocumentQueryPort {

    private static final Logger log = LoggerFactory.getLogger(ReactiveCouchbaseQueryService.class);

    private static final int SAMPLE_RATE = 1000;
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_BASE_DELAY = Duration.ofMillis(50);
    private static final String CB_NAME = "couchbaseQueryBreaker";

    private final ReactiveCluster reactiveCluster;
    private final String keyspace;
    private final Duration queryTimeout;
    private final QueryMetrics metrics;

    public ReactiveCouchbaseQueryService(
            Cluster cluster,
            String keyspace,
            AppTimeoutProperties timeoutProperties,
            QueryMetrics metrics,
            CircuitBreakerRegistry cbRegistry
    ) {
        this.reactiveCluster = Objects.requireNonNull(cluster).reactive();
        this.keyspace = Objects.requireNonNull(keyspace);
        this.metrics = Objects.requireNonNull(metrics);

        this.queryTimeout = Objects.requireNonNull(timeoutProperties).getQuery();

        // Optional, lightweight CB logging (not noisy)
        cbRegistry.circuitBreaker(CB_NAME)
                .getEventPublisher()
                .onStateTransition(e ->
                        log.warn("CircuitBreaker {} -> {}",
                                e.getStateTransition().getFromState(),
                                e.getStateTransition().getToState()));
    }

    @Override
    @Timed(
            value = "couchbase.query.latency",
            histogram = true,
            extraTags = {"operation", "findByCriteria"}
    )
    @CircuitBreaker(name = CB_NAME)
    @Bulkhead(name = "couchbaseQueryBulkhead", type = Bulkhead.Type.SEMAPHORE)
    public Flux<JsonObject> findByCriteria(DocumentCriteria c) {
        Objects.requireNonNull(c);

        String stmt = N1qlQueryBuilder.buildStatement(keyspace, c);
        JsonObject params = N1qlQueryBuilder.buildParameters(c);

        sampleLogQuery(stmt, c);

        return executePrepared(stmt, params)
                .timeout(queryTimeout);
    }

    // ---------------------------------------------------------------------
    // Prepared → fallback → retry
    // ---------------------------------------------------------------------

    private Flux<JsonObject> executePrepared(String stmt, JsonObject params) {
        return reactiveCluster.query(
                        stmt,
                        QueryOptions.queryOptions()
                                .adhoc(false)
                                .parameters(params)
                                .scanConsistency(QueryScanConsistency.NOT_BOUNDED)
                )
                .flatMapMany(ReactiveQueryResult::rowsAsObject)

                // Fallback to adhoc
                .onErrorResume(this::isPreparedError,
                        ex -> retryAdhoc(stmt, params, ex))

                // Retry on transient backend errors
                .retryWhen(backoffRetrySpec("prepared"));
    }

    private Flux<JsonObject> retryAdhoc(String stmt, JsonObject params, Throwable cause) {
        metrics.incrementPreparedFallback();
        log.warn("Prepared invalid → switching to adhoc. cause={}", cause.toString());

        return reactiveCluster.query(
                        stmt,
                        QueryOptions.queryOptions()
                                .adhoc(true)
                                .parameters(params)
                                .scanConsistency(QueryScanConsistency.NOT_BOUNDED)
                )
                .flatMapMany(ReactiveQueryResult::rowsAsObject)
                .retryWhen(backoffRetrySpec("adhoc"));
    }

    // ---------------------------------------------------------------------
    // Retry: exponential backoff + jitter
    // ---------------------------------------------------------------------

    private Retry backoffRetrySpec(String phase) {
        return Retry.backoff(MAX_RETRIES, RETRY_BASE_DELAY)
                .jitter(0.25)
                .filter(this::isRetryable)

                .doBeforeRetry(rs -> {
                    long attempt = rs.totalRetries() + 1;

                    // Manually compute delay (Reactor 3.5 removed rs.backoff())
                    long delay = (long) (RETRY_BASE_DELAY.toMillis()
                            * Math.pow(2, rs.totalRetries())
                            * (1 + ThreadLocalRandom.current().nextDouble(0.25)));

                    log.warn("Retry {} (phase={}, delay={}ms, cause={})",
                            attempt, phase, delay, rs.failure().toString());
                });
    }

    private boolean isRetryable(Throwable ex) {
        if (ex instanceof CallNotPermittedException) return false; // CB open
        return ex instanceof CouchbaseException
                || ex.getCause() instanceof CouchbaseException;
    }

    private boolean isPreparedError(Throwable ex) {
        return ex instanceof PreparedStatementFailureException
                || ex instanceof PlanningFailureException;
    }

    // ---------------------------------------------------------------------
    // Sampled Logging
    // ---------------------------------------------------------------------

    private void sampleLogQuery(String stmt, DocumentCriteria c) {
        if (ThreadLocalRandom.current().nextInt(SAMPLE_RATE) == 0) {
            log.info("Sampled query: stmt='{}', tags={}, attrKey={}, attrValue={}",
                    stmt,
                    c.getTags() != null && !c.getTags().isEmpty(),
                    c.getAttrKey() != null,
                    c.getAttrValue() != null);
        }
    }
}
