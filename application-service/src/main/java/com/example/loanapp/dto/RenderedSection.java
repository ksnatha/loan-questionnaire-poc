package com.example.loanapp.dto;

import com.example.common.template.FieldDefinition;
import com.example.common.template.GridDefinition;
import java.util.List;

public class RenderedSection {
    public String sectionId;
    public String labelKey;
    public List<FieldDefinition> fields;
    public List<GridDefinition> grids;
}
