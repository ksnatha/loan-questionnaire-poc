package com.example.questionnaire.dto;

import com.example.common.template.SectionTemplateJson;

public class UpdateSectionRequest {
    public SectionTemplateJson template;
    public String labelKey; // optional — when present, renames the section's labelKey
}
