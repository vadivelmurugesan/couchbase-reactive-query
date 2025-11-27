package com.github.vadivelmurugesan.couchbase.domain.model;

/**
 * Standard API error response wrapper.
 */
public record ApiError(String error, String message) { }
