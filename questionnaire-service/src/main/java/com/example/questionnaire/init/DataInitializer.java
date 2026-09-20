package com.example.questionnaire.init;

import com.example.common.template.*;
import com.example.questionnaire.repository.*;
import com.example.questionnaire.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SectionTemplateRepository sectionRepo;
    private final TabTemplateRepository tabRepo;
    private final SectionTemplateService sectionService;
    private final TabTemplateService tabService;

    public DataInitializer(SectionTemplateRepository sectionRepo,
                           TabTemplateRepository tabRepo,
                           SectionTemplateService sectionService,
                           TabTemplateService tabService) {
        this.sectionRepo = sectionRepo;
        this.tabRepo = tabRepo;
        this.sectionService = sectionService;
        this.tabService = tabService;
    }

    @Override
    public void run(String... args) {
        seedKeyInformationSection();
        seedAssociatedRecordsSection();
        seedProposalTab();
    }

    private void seedKeyInformationSection() {
        if (sectionRepo.findBySectionIdAndVersion("key-information", 1).isPresent()) return;

        SectionTemplateJson template = new SectionTemplateJson();

        // --- DEDICATED fields (Slice A) ---
        FieldDefinition proposalName = field("proposal_name", FieldType.TEXT, "field.proposal_name",
            StorageDefinition.dedicated("loan_application", "proposal_name"), true);
        ValidationRule minLen = validationRule("proposal-name-min", "proposal_name", ValidationType.MIN_LENGTH);
        minLen.minLength = 3;
        ValidationRule maxLen = validationRule("proposal-name-max", "proposal_name", ValidationType.MAX_LENGTH);
        maxLen.maxLength = 200;
        proposalName.validationRules = List.of(minLen, maxLen);

        FieldDefinition loanAmount = field("loan_amount", FieldType.NUMBER, "field.loan_amount",
            StorageDefinition.dedicated("loan_application", "loan_amount"), true);

        FieldDefinition proposalDesc = field("proposal_description", FieldType.TEXTAREA,
            "field.proposal_description",
            StorageDefinition.dedicated("loan_application", "proposal_description"), false);

        // --- EAV fields (Slice B) ---
        List<FieldOption> yesNo = List.of(
            new FieldOption("YES", "Yes"),
            new FieldOption("NO", "No"));

        FieldDefinition hasExistingRel = field("has_existing_relationship", FieldType.DROPDOWN,
            "field.has_existing_relationship", StorageDefinition.eav(), false);
        hasExistingRel.dropdownSource = DropdownSource.staticSource(yesNo);

        FieldDefinition existingRelId = field("existing_relationship_id", FieldType.TEXT,
            "field.existing_relationship_id", StorageDefinition.eav(), false);
        // Demo 1: show only when has_existing_relationship = YES
        VisibilityRule showWhenYes = new VisibilityRule();
        showWhenYes.ruleId = "show-existing-rel-id";
        showWhenYes.scope = RuleScope.SECTION;
        showWhenYes.targetFieldKey = "existing_relationship_id";
        showWhenYes.show = true;
        showWhenYes.condition = eqCondition("has_existing_relationship", "YES");
        existingRelId.visibilityRules = List.of(showWhenYes);

        FieldDefinition loanPurpose = field("loan_purpose", FieldType.DROPDOWN,
            "field.loan_purpose", StorageDefinition.eav(), false);
        loanPurpose.dropdownSource = DropdownSource.codeSet("LOAN_PURPOSE");

        FieldDefinition loanTermMonths = field("loan_term_months", FieldType.NUMBER,
            "field.loan_term_months", StorageDefinition.eav(), false);

        FieldDefinition repaymentFreq = field("repayment_frequency", FieldType.DROPDOWN,
            "field.repayment_frequency", StorageDefinition.eav(), false);
        repaymentFreq.dropdownSource = DropdownSource.codeSet("REPAYMENT_FREQ");

        FieldDefinition interestRateType = field("interest_rate_type", FieldType.DROPDOWN,
            "field.interest_rate_type", StorageDefinition.eav(), false);
        interestRateType.dropdownSource = DropdownSource.codeSet("INTEREST_RATE_TYPE");

        FieldDefinition rateCapPct = field("rate_cap_percentage", FieldType.NUMBER,
            "field.rate_cap_percentage", StorageDefinition.eav(), false);
        // Demo 2: required only when interest_rate_type = VARIABLE
        ValidationRule conditionalRequired = new ValidationRule();
        conditionalRequired.ruleId = "rate-cap-conditional-required";
        conditionalRequired.scope = RuleScope.SECTION;
        conditionalRequired.targetFieldKey = "rate_cap_percentage";
        conditionalRequired.validationType = ValidationType.REQUIRED;
        conditionalRequired.condition = eqCondition("interest_rate_type", "VARIABLE");
        rateCapPct.validationRules = List.of(conditionalRequired);

        FieldDefinition requiresCollateral = field("requires_collateral", FieldType.DROPDOWN,
            "field.requires_collateral", StorageDefinition.eav(), false);
        requiresCollateral.dropdownSource = DropdownSource.staticSource(yesNo);

        FieldDefinition additionalComments = field("additional_comments", FieldType.TEXTAREA,
            "field.additional_comments", StorageDefinition.eav(), false);

        template.fields = new ArrayList<>(List.of(
            proposalName, loanAmount, proposalDesc,
            hasExistingRel, existingRelId,
            loanPurpose, loanTermMonths, repaymentFreq, interestRateType,
            rateCapPct, requiresCollateral, additionalComments));

        var st = sectionService.create("key-information", "section.key_information", template);
        sectionService.publish("key-information", st.getVersion());
    }

    private void seedAssociatedRecordsSection() {
        if (sectionRepo.findBySectionIdAndVersion("associated-records", 1).isPresent()) return;

        SectionTemplateJson template = new SectionTemplateJson();

        GridDefinition grid = new GridDefinition();
        grid.gridKey = "collateral_properties";
        grid.labelKey = "grid.collateral_properties";

        FieldDefinition propertyAddress = field("property_address", FieldType.TEXT,
            "field.property_address", StorageDefinition.eav(), false);

        FieldDefinition propertyType = field("property_type", FieldType.DROPDOWN,
            "field.property_type", StorageDefinition.eav(), false);
        propertyType.dropdownSource = DropdownSource.codeSet("PROPERTY_TYPE");

        FieldDefinition estimatedValue = field("estimated_value", FieldType.NUMBER,
            "field.estimated_value", StorageDefinition.eav(), false);

        FieldDefinition lienPosition = field("lien_position", FieldType.DROPDOWN,
            "field.lien_position", StorageDefinition.eav(), false);
        lienPosition.dropdownSource = DropdownSource.codeSet("LIEN_POSITION");
        // Demo ROW-scoped rule: lien_position required when property_type = COMMERCIAL
        ValidationRule lienRequired = new ValidationRule();
        lienRequired.ruleId = "lien-required-when-commercial";
        lienRequired.scope = RuleScope.ROW;
        lienRequired.targetFieldKey = "lien_position";
        lienRequired.validationType = ValidationType.REQUIRED;
        lienRequired.condition = eqCondition("property_type", "COMMERCIAL");
        lienPosition.validationRules = List.of(lienRequired);

        FieldDefinition notes = field("notes", FieldType.TEXTAREA,
            "field.notes", StorageDefinition.eav(), false);

        grid.columns = new ArrayList<>(List.of(
            propertyAddress, propertyType, estimatedValue, lienPosition, notes));
        template.grids = new ArrayList<>(List.of(grid));

        var st = sectionService.create("associated-records", "section.associated_records", template);
        sectionService.publish("associated-records", st.getVersion());
    }

    private void seedProposalTab() {
        if (tabRepo.findByTabIdAndVersion("proposal", 1).isPresent()) return;
        var tt = tabService.create("proposal", "tab.proposal",
            List.of(new TabSectionRef("key-information", 1),
                    new TabSectionRef("associated-records", 2)));
        tabService.publish("proposal", tt.getVersion());
    }

    private FieldDefinition field(String key, FieldType type, String labelKey,
                                   StorageDefinition storage, boolean required) {
        FieldDefinition f = new FieldDefinition();
        f.fieldKey = key;
        f.fieldType = type;
        f.labelKey = labelKey;
        f.storage = storage;
        f.required = required;
        return f;
    }

    private ValidationRule validationRule(String ruleId, String targetFieldKey, ValidationType type) {
        ValidationRule r = new ValidationRule();
        r.ruleId = ruleId;
        r.scope = RuleScope.SECTION;
        r.targetFieldKey = targetFieldKey;
        r.validationType = type;
        return r;
    }

    private RuleCondition eqCondition(String fieldKey, String value) {
        RuleCondition c = new RuleCondition();
        c.fieldKey = fieldKey;
        c.operator = RuleOperator.EQUALS;
        c.value = value;
        return c;
    }
}
