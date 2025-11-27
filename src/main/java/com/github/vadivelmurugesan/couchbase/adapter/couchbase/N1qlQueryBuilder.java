package com.github.vadivelmurugesan.couchbase.adapter.couchbase;

import com.couchbase.client.java.json.JsonObject;
import com.github.vadivelmurugesan.couchbase.domain.model.DocumentCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility for building parameterized N1QL (SQL++) statements
 * and a matching parameter JSON payload from a {@link DocumentCriteria}.
 *
 */
public final class N1qlQueryBuilder {

    private N1qlQueryBuilder() {
        // Utility class
    }

    /**
     * Constructs SQL++ query text by dynamically injecting only
     * the predicates required for the given {@link DocumentCriteria}.
     */
    public static String buildStatement(String keyspace, DocumentCriteria c) {
        Objects.requireNonNull(keyspace, "keyspace must not be null");
        Objects.requireNonNull(c, "criteria must not be null");

        List<String> predicates = collectPredicates(c);

        String where = predicates.isEmpty()
                ? "WHERE 1=1"
                : "WHERE " + String.join(" AND ", predicates);

        return "SELECT META(d).id AS id, d.type, d.name, d.age, d.tags " +
                "FROM " + keyspace + " AS d " +
                where + " " +
                "LIMIT 2";
    }

    /**
     * Creates a JsonObject containing only the parameters
     * needed for the active predicates in the criteria.
     */
    public static JsonObject buildParameters(DocumentCriteria c) {
        Objects.requireNonNull(c, "criteria must not be null");

        JsonObject p = JsonObject.create();

        put(p, "type", c.getType());
        put(p, "name", c.getName());
        put(p, "minAge", c.getMinAge());
        put(p, "maxAge", c.getMaxAge());
        put(p, "tags", c.getTags());
        put(p, "attrKey", c.getAttrKey());
        put(p, "attrValue", c.getAttrValue());

        return p;
    }

    // ---------------------------------------------
    // Internal Helpers
    // ---------------------------------------------

    private static List<String> collectPredicates(DocumentCriteria c) {
        List<String> predicates = new ArrayList<>();

        add(predicates, c.getType() != null, "d.type = $type");
        add(predicates, c.getName() != null, "d.name = $name");
        add(predicates, c.getMinAge() != null, "d.age >= $minAge");
        add(predicates, c.getMaxAge() != null, "d.age <= $maxAge");

        // Tags array filter
        if (c.getTags() != null && !c.getTags().isEmpty()) {
            predicates.add("ANY t IN d.tags SATISFIES t IN $tags END");
        }

        // Attribute key/value array filter
        boolean hasKey = c.getAttrKey() != null;
        boolean hasVal = c.getAttrValue() != null;

        if (hasKey && hasVal) {
            predicates.add("ANY a IN d.attributes SATISFIES a.`key` = $attrKey AND a.`value` = $attrValue END");
        } else if (hasKey) {
            predicates.add("ANY a IN d.attributes SATISFIES a.`key` = $attrKey END");
        } else if (hasVal) {
            predicates.add("ANY a IN d.attributes SATISFIES a.`value` = $attrValue END");
        }

        return predicates;
    }

    /**
     * Adds predicate only when the condition is true.
     */
    private static void add(List<String> list, boolean condition, String predicate) {
        if (condition) {
            list.add(predicate);
        }
    }

    /**
     * Adds a JSON param only when non-null.
     */
    private static void put(JsonObject json, String key, Object value) {
        if (value != null) {
            json.put(key, value);
        }
    }
}