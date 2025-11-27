package com.github.vadivelmurugesan.couchbase.adapter.web;

/**
 * Thrown when query criteria are too broad or invalid for safe execution.
 */
public class InvalidCriteriaException extends RuntimeException {

    public InvalidCriteriaException(String message) {
        super(message);
    }
}
