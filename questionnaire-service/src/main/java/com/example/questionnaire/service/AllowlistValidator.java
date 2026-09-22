package com.example.questionnaire.service;

import com.example.common.template.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AllowlistValidator {

    static final Set<String> ALLOWLIST = Set.of(
        // loan_application
        "loan_application.proposal_name",
        "loan_application.loan_amount",
        "loan_application.proposal_description",
        // loan_financial_summary
        "loan_financial_summary.net_worth",
        "loan_financial_summary.annual_revenue",
        "loan_financial_summary.total_liabilities",
        "loan_financial_summary.credit_score",
        // loan_compliance_record
        "loan_compliance_record.aml_check_status",
        "loan_compliance_record.aml_check_date",
        "loan_compliance_record.pep_status",
        "loan_compliance_record.sanctions_status",
        // loan_employment_detail
        "loan_employment_detail.employment_status",
        "loan_employment_detail.employer_name",
        "loan_employment_detail.gross_annual_income",
        "loan_employment_detail.net_monthly_income",
        // loan_property_info
        "loan_property_info.property_address_full",
        "loan_property_info.property_state",
        "loan_property_info.property_postcode",
        "loan_property_info.property_purchase_price",
        "loan_property_info.property_valuation",
        // loan_guarantor (DEDICATED grid columns)
        "loan_guarantor.guarantor_name",
        "loan_guarantor.date_of_birth",
        "loan_guarantor.contact_number",
        "loan_guarantor.relationship",
        "loan_guarantor.guaranteed_amount",
        "loan_guarantor.independent_advice"
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
            if (col.storage != null && col.storage.type == StorageType.DEDICATED) {
                String ref = col.storage.tableName + "." + col.storage.columnName;
                if (!ALLOWLIST.contains(ref)) {
                    throw new IllegalArgumentException(
                        "DEDICATED grid column not in allowlist: " + ref);
                }
            }
        }
    }
}
