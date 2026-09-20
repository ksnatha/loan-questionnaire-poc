package com.example.questionnaire.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "section_template",
       uniqueConstraints = @UniqueConstraint(columnNames = {"section_id", "version"}))
public class SectionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private String sectionId;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status = TemplateStatus.DRAFT;

    @Column(name = "label_key", nullable = false)
    private String labelKey;

    @Column(name = "has_grid", nullable = false)
    private boolean hasGrid;

    @Column(name = "dedicated_column_refs", length = 1000)
    private String dedicatedColumnRefs;

    @Lob
    @Column(name = "template_json", nullable = false)
    private String templateJson;

    public Long getId() { return id; }
    public String getSectionId() { return sectionId; }
    public void setSectionId(String v) { this.sectionId = v; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }
    public TemplateStatus getStatus() { return status; }
    public void setStatus(TemplateStatus v) { this.status = v; }
    public String getLabelKey() { return labelKey; }
    public void setLabelKey(String v) { this.labelKey = v; }
    public boolean isHasGrid() { return hasGrid; }
    public void setHasGrid(boolean v) { this.hasGrid = v; }
    public String getDedicatedColumnRefs() { return dedicatedColumnRefs; }
    public void setDedicatedColumnRefs(String v) { this.dedicatedColumnRefs = v; }
    public String getTemplateJson() { return templateJson; }
    public void setTemplateJson(String v) { this.templateJson = v; }
}
