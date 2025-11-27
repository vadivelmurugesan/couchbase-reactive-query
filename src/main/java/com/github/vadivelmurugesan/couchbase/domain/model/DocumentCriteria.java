package com.github.vadivelmurugesan.couchbase.domain.model;

import java.util.List;

/**
 * Dynamic criteria for querying documents.
 * All fields are optional; null means "not filtered on".
 */
public final class DocumentCriteria {

    private String type;
    private String name;
    private Integer minAge;
    private Integer maxAge;
    private List<String> tags;

    // Nested array criteria: attributes[*].key / attributes[*].value
    private String attrKey;
    private String attrValue;

    public String getType() { return type; }
    public DocumentCriteria setType(String type) { this.type = type; return this; }

    public String getName() { return name; }
    public DocumentCriteria setName(String name) { this.name = name; return this; }

    public Integer getMinAge() { return minAge; }
    public DocumentCriteria setMinAge(Integer minAge) { this.minAge = minAge; return this; }

    public Integer getMaxAge() { return maxAge; }
    public DocumentCriteria setMaxAge(Integer maxAge) { this.maxAge = maxAge; return this; }

    public List<String> getTags() { return tags; }
    public DocumentCriteria setTags(List<String> tags) { this.tags = tags; return this; }

    public String getAttrKey() { return attrKey; }
    public DocumentCriteria setAttrKey(String attrKey) { this.attrKey = attrKey; return this; }

    public String getAttrValue() { return attrValue; }
    public DocumentCriteria setAttrValue(String attrValue) { this.attrValue = attrValue; return this; }
}
