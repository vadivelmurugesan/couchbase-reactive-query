package com.github.vadivelmurugesan.couchbase.adapter.web;

import com.couchbase.client.core.error.CouchbaseException;
import com.github.vadivelmurugesan.couchbase.domain.model.ApiError;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Global reactive exception handler for REST endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCriteriaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ApiError> handleInvalidCriteria(InvalidCriteriaException ex) {
        return Mono.just(new ApiError("INVALID_CRITERIA", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSchemaException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiError> handleInvalidSchema(InvalidSchemaException ex) {
        return Mono.just(new ApiError("INVALID_SCHEMA", ex.getMessage()));
    }

    @ExceptionHandler(RequestNotPermitted.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Mono<ApiError> handleRateLimit(RequestNotPermitted ex) {
        return Mono.just(new ApiError("RATE_LIMITED", "Too many requests, please slow down"));
    }

    @ExceptionHandler(CouchbaseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiError> handleCouchbaseException(CouchbaseException ex) {
        log.error("Couchbase error", ex);
        String msg = ex.getMessage() != null ? ex.getMessage() : "Unexpected Couchbase error";
        return Mono.just(new ApiError("COUCHBASE_ERROR", msg));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ApiError> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return Mono.just(new ApiError("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
