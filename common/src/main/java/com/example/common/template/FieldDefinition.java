package com.example.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldDefinition {

    public String fieldKey;
    public FieldType fieldType;
    public String labelKey;
    public StorageDefinition storage;
    public DropdownSource dropdownSource;
    public boolean required;
    public List<VisibilityRule> visibilityRules = new ArrayList<>();
    public List<ValidationRule> validationRules = new ArrayList<>();

    // Populated at render time by application-service for CODE_SET / EXTERNAL sources.
    // Not stored in template_json.
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<FieldOption> resolvedOptions = new ArrayList<>();

    public FieldDefinition() {}
}
