package com.example.questionnaire.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tab_template",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tab_id", "version"}))
public class TabTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tab_id", nullable = false)
    private String tabId;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status = TemplateStatus.DRAFT;

    @Column(name = "label_key", nullable = false)
    private String labelKey;

    @Lob
    @Column(name = "template_json", nullable = false)
    private String templateJson;

    public Long getId() { return id; }
    public String getTabId() { return tabId; }
    public void setTabId(String v) { this.tabId = v; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }
    public TemplateStatus getStatus() { return status; }
    public void setStatus(TemplateStatus v) { this.status = v; }
    public String getLabelKey() { return labelKey; }
    public void setLabelKey(String v) { this.labelKey = v; }
    public String getTemplateJson() { return templateJson; }
    public void setTemplateJson(String v) { this.templateJson = v; }
}
