package com.example.questionnaire.service;

import com.example.common.template.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AllowlistValidatorTest {

    private final AllowlistValidator validator = new AllowlistValidator();

    @Test
    void rejectsUnlistedDedicatedField() {
        SectionTemplateJson template = sectionWith(
            field("bad_field", StorageDefinition.dedicated("some_table", "bad_column")));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(template),
            "Should reject DEDICATED field not in allowlist");
    }

    @Test
    void rejectsDuplicateFieldKeyInFields() {
        SectionTemplateJson template = sectionWith(
            field("proposal_name", StorageDefinition.dedicated("loan_application", "proposal_name")),
            field("proposal_name", StorageDefinition.dedicated("loan_application", "loan_amount")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validator.validate(template));
        assertTrue(ex.getMessage().contains("Duplicate fieldKey"));
    }

    @Test
    void rejectsDuplicateFieldKeyInGridColumns() {
        SectionTemplateJson template = new SectionTemplateJson();
        GridDefinition grid = new GridDefinition();
        grid.gridKey = "test_grid";
        grid.columns = new ArrayList<>(List.of(
            field("col_a", StorageDefinition.eav()),
            field("col_a", StorageDefinition.eav())));
        template.grids = new ArrayList<>(List.of(grid));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validator.validate(template));
        assertTrue(ex.getMessage().contains("Duplicate fieldKey in grid test_grid"));
    }

    @Test
    void acceptsAllowlistedDedicatedFields() {
        SectionTemplateJson template = sectionWith(
            field("proposal_name", StorageDefinition.dedicated("loan_application", "proposal_name")),
            field("loan_amount", StorageDefinition.dedicated("loan_application", "loan_amount")),
            field("proposal_description",
                  StorageDefinition.dedicated("loan_application", "proposal_description")));
        assertDoesNotThrow(() -> validator.validate(template));
    }

    @Test
    void acceptsEavFieldsWithoutAllowlistCheck() {
        SectionTemplateJson template = sectionWith(
            field("some_eav_field", StorageDefinition.eav()));
        assertDoesNotThrow(() -> validator.validate(template));
    }

    private SectionTemplateJson sectionWith(FieldDefinition... fields) {
        SectionTemplateJson t = new SectionTemplateJson();
        t.fields = new ArrayList<>(List.of(fields));
        return t;
    }

    private FieldDefinition field(String key, StorageDefinition storage) {
        FieldDefinition f = new FieldDefinition();
        f.fieldKey = key;
        f.fieldType = FieldType.TEXT;
        f.labelKey = "field." + key;
        f.storage = storage;
        return f;
    }
}
