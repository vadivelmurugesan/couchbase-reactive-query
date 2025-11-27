package com.github.vadivelmurugesan.couchbase.adapter.web;

import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.github.vadivelmurugesan.couchbase.domain.DocumentQueryPort;
import com.github.vadivelmurugesan.couchbase.domain.model.DocumentCriteria;
import com.github.vadivelmurugesan.couchbase.domain.model.DocumentResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * Reactive REST controller exposing dynamic Couchbase document queries.
 *
 */
@RestController
@RequestMapping("/api/documents")
public final class DocumentQueryController {

    private final DocumentQueryPort service;

    public DocumentQueryController(DocumentQueryPort service) {
        this.service = Objects.requireNonNull(service, "DocumentQueryPort must not be null");
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    @PostMapping("/search")
    @RateLimiter(name = "searchRateLimiter")
    public Flux<DocumentResponse> search(@RequestBody Mono<DocumentCriteria> criteriaMono) {
        return criteriaMono
                .switchIfEmpty(Mono.error(new InvalidCriteriaException("Request body is required")))
                .flatMap(this::validateCriteria)
                .flatMapMany(service::findByCriteria)
                .map(DocumentQueryController::mapRowToResponse);
    }

    // ---------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------

    private Mono<DocumentCriteria> validateCriteria(DocumentCriteria c) {
        Objects.requireNonNull(c, "DocumentCriteria must not be null");

        boolean hasType = c.getType() != null && !c.getType().isBlank();

        boolean hasOther =
                hasNonBlank(c.getName()) ||
                        c.getMinAge() != null ||
                        c.getMaxAge() != null ||
                        listNotEmpty(c.getTags()) ||
                        hasNonBlank(c.getAttrKey()) ||
                        hasNonBlank(c.getAttrValue());

        if (!hasType || !hasOther) {
            return Mono.error(new InvalidCriteriaException(
                    "Invalid query: 'type' and at least one additional filter are required."));
        }
        return Mono.just(c);
    }

    private boolean hasNonBlank(String s) {
        return s != null && !s.isBlank();
    }

    private boolean listNotEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    // ---------------------------------------------------------------------
    // Row Mapping
    // ---------------------------------------------------------------------

    private static DocumentResponse mapRowToResponse(JsonObject row) {
        Objects.requireNonNull(row, "Row JsonObject must not be null");

        validateSchema(row);

        String id = row.getString("id");
        String type = row.getString("type");
        String name = row.getString("name");

        Integer age = row.containsKey("age") ? row.getInt("age") : null;

        List<String> tags = extractTags(row);

        return new DocumentResponse(id, type, name, age, tags);
    }

    private static void validateSchema(JsonObject row) {
        if (row.containsKey("tags")) {
            if (!(row.get("tags") instanceof JsonArray)) {
                throw new InvalidSchemaException("Invalid schema: 'tags' must be an array.");
            }
        }
    }

    private static List<String> extractTags(JsonObject row) {
        if (!row.containsKey("tags")) {
            return null;
        }

        JsonArray arr = row.getArray("tags");
        return arr.toList().stream()
                .map(o -> {
                    if (!(o instanceof String)) {
                        throw new InvalidSchemaException("Invalid schema: 'tags' array contains non-string value.");
                    }
                    return (String) o;
                })
                .toList();
    }
}
