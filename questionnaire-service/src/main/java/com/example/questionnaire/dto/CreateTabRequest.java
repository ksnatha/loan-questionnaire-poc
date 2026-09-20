package com.example.questionnaire.dto;

import com.example.common.template.TabSectionRef;
import java.util.List;

public class CreateTabRequest {
    public String tabId;
    public String labelKey;
    public List<TabSectionRef> sections;
}
