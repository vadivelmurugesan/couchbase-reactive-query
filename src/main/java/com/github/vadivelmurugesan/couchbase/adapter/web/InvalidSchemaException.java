package com.github.vadivelmurugesan.couchbase.adapter.web;

/**
 * Thrown when a document in Couchbase does not conform to the expected schema.
 * Indicates data corruption or schema drift.
 */
public class InvalidSchemaException extends RuntimeException {

    public InvalidSchemaException(String message) {
        super(message);
    }
}
