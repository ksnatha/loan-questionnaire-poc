package com.example.questionnaire.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tab_template_section")
public class TabTemplateSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tab_template_id", nullable = false)
    private Long tabTemplateId;

    @Column(name = "section_id", nullable = false, length = 100)
    private String sectionId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public Long getId() { return id; }
    public Long getTabTemplateId() { return tabTemplateId; }
    public void setTabTemplateId(Long v) { this.tabTemplateId = v; }
    public String getSectionId() { return sectionId; }
    public void setSectionId(String v) { this.sectionId = v; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int v) { this.displayOrder = v; }
}
