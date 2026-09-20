package com.example.questionnaire.dto;

import com.example.common.template.SectionTemplateJson;
import java.util.List;

public class SectionTemplateResponse {
    public String sectionId;
    public Integer version;
    public String status;
    public String labelKey;
    public boolean hasGrid;
    public List<String> dedicatedColumnRefs;
    public SectionTemplateJson template;
}
