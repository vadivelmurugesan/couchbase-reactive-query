package com.github.vadivelmurugesan.couchbase.config;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Couchbase bucket/scope/collection wiring.
 */
@Configuration
public class CouchbaseHaConfig {

    @Value("${app.couchbase.bucket}")
    private String bucket;

    @Value("${app.couchbase.scope}")
    private String scope;

    @Value("${app.couchbase.collection}")
    private String collection;

    @Bean
    public String keyspace() {
        // `bucket`.`scope`.`collection` per Couchbase 7.x SQL++
        return String.format("`%s`.`%s`.`%s`", bucket, scope, collection);
    }

    @Bean
    public Scope scope(Cluster cluster) {
        return cluster.bucket(bucket).scope(scope);
    }

    @Bean
    public Collection collection(Scope scope) {
        return scope.collection(collection);
    }
}
