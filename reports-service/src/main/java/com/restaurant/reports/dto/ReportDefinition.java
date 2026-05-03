package com.restaurant.reports.dto;

import java.util.List;

public class ReportDefinition {
    private String id;
    private String label;
    private String dataSource;
    private List<SectionDefinition> sections;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public List<SectionDefinition> getSections() { return sections; }
    public void setSections(List<SectionDefinition> sections) { this.sections = sections; }
}
