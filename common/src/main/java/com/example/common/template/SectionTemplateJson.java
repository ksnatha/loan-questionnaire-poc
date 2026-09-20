package com.example.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SectionTemplateJson {

    public List<FieldDefinition> fields = new ArrayList<>();
    public List<GridDefinition> grids = new ArrayList<>(); // empty for non-grid sections

    public SectionTemplateJson() {}
}
