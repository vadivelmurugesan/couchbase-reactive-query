package com.github.vadivelmurugesan.couchbase.domain.model;

import java.util.List;

/**
 * API response DTO for a document.
 */
public class DocumentResponse {

    private String id;
    private String type;
    private String name;
    private Integer age;
    private List<String> tags;

    public DocumentResponse() {
    }

    public DocumentResponse(String id, String type, String name, Integer age, List<String> tags) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.age = age;
        this.tags = tags;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
