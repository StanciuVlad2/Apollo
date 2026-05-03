package com.restaurant.reports.dto;

import java.util.List;

public class SectionDefinition {
    private String type;
    private String title;
    private List<String> fields;
    private Integer limit;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<String> getFields() { return fields; }
    public void setFields(List<String> fields) { this.fields = fields; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
