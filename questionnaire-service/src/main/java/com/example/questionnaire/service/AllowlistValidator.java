package com.example.questionnaire.service;

import com.example.common.template.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AllowlistValidator {

    static final Set<String> ALLOWLIST = Set.of(
        "loan_application.proposal_name",
        "loan_application.loan_amount",
        "loan_application.proposal_description"
    );

    public void validate(SectionTemplateJson template) {
        validateFields(template.fields);
        for (GridDefinition grid : template.grids) {
            validateGridColumns(grid);
        }
    }

    private void validateFields(List<FieldDefinition> fields) {
        Set<String> seen = new HashSet<>();
        for (FieldDefinition f : fields) {
            if (!seen.add(f.fieldKey)) {
                throw new IllegalArgumentException("Duplicate fieldKey in fields: " + f.fieldKey);
            }
            if (f.storage != null && f.storage.type == StorageType.DEDICATED) {
                String ref = f.storage.tableName + "." + f.storage.columnName;
                if (!ALLOWLIST.contains(ref)) {
                    throw new IllegalArgumentException("DEDICATED field not in allowlist: " + ref);
                }
            }
        }
    }

    private void validateGridColumns(GridDefinition grid) {
        Set<String> seen = new HashSet<>();
        for (FieldDefinition col : grid.columns) {
            if (!seen.add(col.fieldKey)) {
                throw new IllegalArgumentException(
                    "Duplicate fieldKey in grid " + grid.gridKey + ": " + col.fieldKey);
            }
        }
    }
}
