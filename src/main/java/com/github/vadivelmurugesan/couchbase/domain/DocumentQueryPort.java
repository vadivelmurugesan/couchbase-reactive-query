package com.github.vadivelmurugesan.couchbase.domain;

import com.couchbase.client.java.json.JsonObject;
import com.github.vadivelmurugesan.couchbase.domain.model.DocumentCriteria;
import reactor.core.publisher.Flux;

/**
 * Port for querying documents based on dynamic criteria.
 */
public interface DocumentQueryPort {

    /**
     * Execute a dynamic query based on the given criteria.
     *
     * @param criteria dynamic filter criteria
     * @return a Flux of JsonObject rows (each row representing a document)
     */
    Flux<JsonObject> findByCriteria(DocumentCriteria criteria);
}

