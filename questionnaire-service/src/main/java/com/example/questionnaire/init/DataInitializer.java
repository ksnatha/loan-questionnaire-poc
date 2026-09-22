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

    // ── Reusable dropdown option lists ────────────────────────────────────────────

    private static final List<FieldOption> YES_NO = List.of(
        new FieldOption("YES", "Yes"),
        new FieldOption("NO",  "No"));

    private static final List<FieldOption> YES_NO_NA = List.of(
        new FieldOption("YES", "Yes"),
        new FieldOption("NO",  "No"),
        new FieldOption("NA",  "N/A"));

    // ── Entry point ───────────────────────────────────────────────────────────────

    @Override
    public void run(String... args) {
        seedKeyInformationSection();
        seedAssociatedRecordsSection();
        seedGuarantorsSection();
        seedFinancialDetailsSection();
        seedComplianceAmlSection();
        seedEmploymentIncomeSection();
        seedPropertyDetailsSection();
        seedProposalTab();
    }

    // ── Key Information (existing) ────────────────────────────────────────────────

    private void seedKeyInformationSection() {
        if (sectionRepo.findBySectionIdAndVersion("key-information", 1).isPresent()) return;

        SectionTemplateJson template = new SectionTemplateJson();

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

        FieldDefinition hasExistingRel = field("has_existing_relationship", FieldType.DROPDOWN,
            "field.has_existing_relationship", StorageDefinition.eav(), false);
        hasExistingRel.dropdownSource = DropdownSource.staticSource(YES_NO);

        FieldDefinition existingRelId = field("existing_relationship_id", FieldType.TEXT,
            "field.existing_relationship_id", StorageDefinition.eav(), false);
        existingRelId.visibilityRules = List.of(
            showWhen("show-existing-rel-id", "existing_relationship_id", "has_existing_relationship", "YES"));

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
        ValidationRule conditionalRequired = validationRule(
            "rate-cap-conditional-required", "rate_cap_percentage", ValidationType.REQUIRED);
        conditionalRequired.condition = eqCondition("interest_rate_type", "VARIABLE");
        rateCapPct.validationRules = List.of(conditionalRequired);

        FieldDefinition requiresCollateral = field("requires_collateral", FieldType.DROPDOWN,
            "field.requires_collateral", StorageDefinition.eav(), false);
        requiresCollateral.dropdownSource = DropdownSource.staticSource(YES_NO);

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

    // ── Associated Records / Collateral (existing EAV grid) ──────────────────────

    private void seedAssociatedRecordsSection() {
        if (sectionRepo.findBySectionIdAndVersion("associated-records", 1).isPresent()) return;

        SectionTemplateJson template = new SectionTemplateJson();

        GridDefinition grid = new GridDefinition();
        grid.gridKey = "collateral_properties";
        grid.labelKey = "grid.collateral_properties";
        // gridStorageType defaults to "EAV"

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
        ValidationRule lienRequired = validationRule(
            "lien-required-when-commercial", "lien_position", ValidationType.REQUIRED);
        lienRequired.scope = RuleScope.ROW;
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

    // ── Guarantors (DEDICATED grid — new) ────────────────────────────────────────

    private void seedGuarantorsSection() {
        if (sectionRepo.findBySectionIdAndVersion("guarantors", 1).isPresent()) return;

        SectionTemplateJson template = new SectionTemplateJson();

        GridDefinition grid = new GridDefinition();
        grid.gridKey = "guarantors_list";
        grid.labelKey = "grid.guarantors_list";
        grid.gridStorageType = "DEDICATED";
        grid.gridBackingTable = "loan_guarantor";

        FieldDefinition name = field("guarantor_name", FieldType.TEXT, "field.guarantor_name",
            StorageDefinition.dedicated("loan_guarantor", "guarantor_name"), true);

        FieldDefinition dob = field("date_of_birth", FieldType.TEXT, "field.date_of_birth",
            StorageDefinition.dedicated("loan_guarantor", "date_of_birth"), true);

        FieldDefinition contact = field("contact_number", FieldType.TEXT, "field.contact_number",
            StorageDefinition.dedicated("loan_guarantor", "contact_number"), false);

        FieldDefinition relationship = field("relationship", FieldType.DROPDOWN,
            "field.relationship", StorageDefinition.dedicated("loan_guarantor", "relationship"), true);
        relationship.dropdownSource = DropdownSource.codeSet("GUARANTOR_RELATIONSHIP");

        FieldDefinition amount = field("guaranteed_amount", FieldType.NUMBER,
            "field.guaranteed_amount",
            StorageDefinition.dedicated("loan_guarantor", "guaranteed_amount"), true);

        FieldDefinition advice = field("independent_advice", FieldType.DROPDOWN,
            "field.independent_advice",
            StorageDefinition.dedicated("loan_guarantor", "independent_advice"), true);
        advice.dropdownSource = DropdownSource.staticSource(List.of(
            new FieldOption("YES",     "Yes"),
            new FieldOption("NO",      "No"),
            new FieldOption("PENDING", "Pending")));

        grid.columns = new ArrayList<>(List.of(name, dob, contact, relationship, amount, advice));
        template.grids = new ArrayList<>(List.of(grid));

        var st = sectionService.create("guarantors", "section.guarantors", template);
        sectionService.publish("guarantors", st.getVersion());
    }

    // ── Financial Details ─────────────────────────────────────────────────────────

    private void seedFinancialDetailsSection() {
        if (sectionRepo.findBySectionIdAndVersion("financial-details", 1).isPresent()) return;

        List<FieldDefinition> fields = new ArrayList<>();

        // DEDICATED fields
        fields.add(field("net_worth", FieldType.NUMBER, "field.net_worth",
            StorageDefinition.dedicated("loan_financial_summary", "net_worth"), true));
        fields.add(field("annual_revenue", FieldType.NUMBER, "field.annual_revenue",
            StorageDefinition.dedicated("loan_financial_summary", "annual_revenue"), true));
        fields.add(field("total_liabilities", FieldType.NUMBER, "field.total_liabilities",
            StorageDefinition.dedicated("loan_financial_summary", "total_liabilities"), true));
        fields.add(field("credit_score", FieldType.NUMBER, "field.credit_score",
            StorageDefinition.dedicated("loan_financial_summary", "credit_score"), false));

        // EAV fields
        fields.add(field("primary_bank", FieldType.TEXT, "field.primary_bank",
            StorageDefinition.eav(), false));

        FieldDefinition yearsInBiz = field("years_in_business", FieldType.NUMBER,
            "field.years_in_business", StorageDefinition.eav(), true);
        fields.add(yearsInBiz);

        fields.add(field("existing_loan_count", FieldType.NUMBER, "field.existing_loan_count",
            StorageDefinition.eav(), false));

        FieldDefinition fiscalYearEnd = field("fiscal_year_end", FieldType.DROPDOWN,
            "field.fiscal_year_end", StorageDefinition.eav(), false);
        fiscalYearEnd.dropdownSource = DropdownSource.codeSet("FISCAL_YEAR_END");
        fields.add(fiscalYearEnd);

        FieldDefinition bizStructure = field("business_structure", FieldType.DROPDOWN,
            "field.business_structure", StorageDefinition.eav(), false);
        bizStructure.dropdownSource = DropdownSource.codeSet("BUSINESS_STRUCTURE");
        fields.add(bizStructure);

        // Yes/No/N-A questions with conditional comment
        addYesNoNaQuestion(fields, "financial_statements_provided",
            "field.financial_statements_provided", true);
        addYesNoNaQuestion(fields, "credit_threshold_met",
            "field.credit_threshold_met", true);
        addYesNoNaQuestion(fields, "liabilities_disclosed",
            "field.liabilities_disclosed", true);

        SectionTemplateJson template = new SectionTemplateJson();
        template.fields = fields;
        var st = sectionService.create("financial-details", "section.financial_details", template);
        sectionService.publish("financial-details", st.getVersion());
    }

    // ── Compliance / AML ──────────────────────────────────────────────────────────

    private void seedComplianceAmlSection() {
        if (sectionRepo.findBySectionIdAndVersion("compliance-aml", 1).isPresent()) return;

        List<FieldDefinition> fields = new ArrayList<>();

        // DEDICATED fields
        FieldDefinition amlStatus = field("aml_check_status", FieldType.DROPDOWN,
            "field.aml_check_status",
            StorageDefinition.dedicated("loan_compliance_record", "aml_check_status"), true);
        amlStatus.dropdownSource = DropdownSource.staticSource(List.of(
            new FieldOption("PENDING", "Pending"),
            new FieldOption("PASS",    "Pass"),
            new FieldOption("FAIL",    "Fail"),
            new FieldOption("REVIEW",  "Under Review")));
        fields.add(amlStatus);

        fields.add(field("aml_check_date", FieldType.TEXT, "field.aml_check_date",
            StorageDefinition.dedicated("loan_compliance_record", "aml_check_date"), false));

        FieldDefinition pepStatus = field("pep_status", FieldType.DROPDOWN, "field.pep_status",
            StorageDefinition.dedicated("loan_compliance_record", "pep_status"), true);
        pepStatus.dropdownSource = DropdownSource.staticSource(List.of(
            new FieldOption("PENDING", "Pending"),
            new FieldOption("CLEAR",   "Clear"),
            new FieldOption("HIT",     "Hit")));
        fields.add(pepStatus);

        FieldDefinition sanctionsStatus = field("sanctions_status", FieldType.DROPDOWN,
            "field.sanctions_status",
            StorageDefinition.dedicated("loan_compliance_record", "sanctions_status"), true);
        sanctionsStatus.dropdownSource = DropdownSource.staticSource(List.of(
            new FieldOption("PENDING", "Pending"),
            new FieldOption("CLEAR",   "Clear"),
            new FieldOption("HIT",     "Hit")));
        fields.add(sanctionsStatus);

        // EAV fields
        FieldDefinition country = field("country_of_incorporation", FieldType.DROPDOWN,
            "field.country_of_incorporation", StorageDefinition.eav(), true);
        country.dropdownSource = DropdownSource.codeSet("COUNTRY");
        fields.add(country);

        FieldDefinition sourceOfFunds = field("source_of_funds", FieldType.DROPDOWN,
            "field.source_of_funds", StorageDefinition.eav(), true);
        sourceOfFunds.dropdownSource = DropdownSource.codeSet("SOURCE_OF_FUNDS");
        fields.add(sourceOfFunds);

        fields.add(field("years_as_customer", FieldType.NUMBER, "field.years_as_customer",
            StorageDefinition.eav(), false));

        FieldDefinition bizType = field("business_type", FieldType.DROPDOWN,
            "field.business_type", StorageDefinition.eav(), false);
        bizType.dropdownSource = DropdownSource.codeSet("BUSINESS_TYPE");
        fields.add(bizType);

        // Yes/No/N-A questions
        addYesNoNaQuestion(fields, "aml_completed",              "field.aml_completed",             true);
        addYesNoNaQuestion(fields, "pep_screening_done",         "field.pep_screening_done",        true);
        addYesNoNaQuestion(fields, "sanctions_clear",            "field.sanctions_clear",           true);
        addYesNoNaQuestion(fields, "beneficial_owner_identified","field.beneficial_owner_identified",true);

        SectionTemplateJson template = new SectionTemplateJson();
        template.fields = fields;
        var st = sectionService.create("compliance-aml", "section.compliance_aml", template);
        sectionService.publish("compliance-aml", st.getVersion());
    }

    // ── Employment & Income ───────────────────────────────────────────────────────

    private void seedEmploymentIncomeSection() {
        if (sectionRepo.findBySectionIdAndVersion("employment-income", 1).isPresent()) return;

        List<FieldDefinition> fields = new ArrayList<>();

        // DEDICATED fields
        FieldDefinition empStatus = field("employment_status", FieldType.DROPDOWN,
            "field.employment_status",
            StorageDefinition.dedicated("loan_employment_detail", "employment_status"), true);
        empStatus.dropdownSource = DropdownSource.staticSource(List.of(
            new FieldOption("EMPLOYED",      "Employed"),
            new FieldOption("SELF_EMPLOYED", "Self-Employed"),
            new FieldOption("UNEMPLOYED",    "Unemployed"),
            new FieldOption("RETIRED",       "Retired")));
        fields.add(empStatus);

        FieldDefinition employerName = field("employer_name", FieldType.TEXT, "field.employer_name",
            StorageDefinition.dedicated("loan_employment_detail", "employer_name"), false);
        // Conditionally required when employment_status = EMPLOYED
        ValidationRule empNameReq = validationRule(
            "employer-name-req-when-employed", "employer_name", ValidationType.REQUIRED);
        empNameReq.condition = eqCondition("employment_status", "EMPLOYED");
        employerName.validationRules = List.of(empNameReq);
        fields.add(employerName);

        fields.add(field("gross_annual_income", FieldType.NUMBER, "field.gross_annual_income",
            StorageDefinition.dedicated("loan_employment_detail", "gross_annual_income"), true));
        fields.add(field("net_monthly_income", FieldType.NUMBER, "field.net_monthly_income",
            StorageDefinition.dedicated("loan_employment_detail", "net_monthly_income"), true));

        // EAV fields
        fields.add(field("occupation", FieldType.TEXT, "field.occupation",
            StorageDefinition.eav(), false));
        fields.add(field("years_at_current_job", FieldType.NUMBER, "field.years_at_current_job",
            StorageDefinition.eav(), false));

        FieldDefinition incomeSource = field("income_source", FieldType.DROPDOWN,
            "field.income_source", StorageDefinition.eav(), true);
        incomeSource.dropdownSource = DropdownSource.codeSet("INCOME_SOURCE");
        fields.add(incomeSource);

        fields.add(field("secondary_income", FieldType.NUMBER, "field.secondary_income",
            StorageDefinition.eav(), false));
        fields.add(field("tax_return_year", FieldType.NUMBER, "field.tax_return_year",
            StorageDefinition.eav(), false));

        // Yes/No/N-A questions
        addYesNoNaQuestion(fields, "income_verified",     "field.income_verified",     true);
        addYesNoNaQuestion(fields, "payslips_provided",   "field.payslips_provided",   true);
        addYesNoNaQuestion(fields, "tax_returns_provided","field.tax_returns_provided",true);

        SectionTemplateJson template = new SectionTemplateJson();
        template.fields = fields;
        var st = sectionService.create("employment-income", "section.employment_income", template);
        sectionService.publish("employment-income", st.getVersion());
    }

    // ── Property Details ──────────────────────────────────────────────────────────

    private void seedPropertyDetailsSection() {
        if (sectionRepo.findBySectionIdAndVersion("property-details", 1).isPresent()) return;

        List<FieldDefinition> fields = new ArrayList<>();

        // DEDICATED fields
        fields.add(field("property_address_full", FieldType.TEXTAREA,
            "field.property_address_full",
            StorageDefinition.dedicated("loan_property_info", "property_address_full"), true));
        fields.add(field("property_state", FieldType.TEXT, "field.property_state",
            StorageDefinition.dedicated("loan_property_info", "property_state"), true));
        fields.add(field("property_postcode", FieldType.TEXT, "field.property_postcode",
            StorageDefinition.dedicated("loan_property_info", "property_postcode"), true));
        fields.add(field("property_purchase_price", FieldType.NUMBER,
            "field.property_purchase_price",
            StorageDefinition.dedicated("loan_property_info", "property_purchase_price"), true));
        fields.add(field("property_valuation", FieldType.NUMBER, "field.property_valuation",
            StorageDefinition.dedicated("loan_property_info", "property_valuation"), false));

        // EAV fields
        FieldDefinition usage = field("property_usage", FieldType.DROPDOWN,
            "field.property_usage", StorageDefinition.eav(), true);
        usage.dropdownSource = DropdownSource.codeSet("PROPERTY_USAGE");
        fields.add(usage);

        FieldDefinition zoning = field("property_zoning", FieldType.DROPDOWN,
            "field.property_zoning", StorageDefinition.eav(), false);
        zoning.dropdownSource = DropdownSource.codeSet("ZONING_TYPE");
        fields.add(zoning);

        fields.add(field("property_age_years", FieldType.NUMBER, "field.property_age_years",
            StorageDefinition.eav(), false));
        fields.add(field("land_area_sqm", FieldType.NUMBER, "field.land_area_sqm",
            StorageDefinition.eav(), false));
        fields.add(field("floor_area_sqm", FieldType.NUMBER, "field.floor_area_sqm",
            StorageDefinition.eav(), false));
        fields.add(field("number_of_bedrooms", FieldType.NUMBER, "field.number_of_bedrooms",
            StorageDefinition.eav(), false));

        // Yes/No/N-A questions
        addYesNoNaQuestion(fields, "valuation_completed",       "field.valuation_completed",      true);
        addYesNoNaQuestion(fields, "title_search_done",         "field.title_search_done",        true);
        addYesNoNaQuestion(fields, "building_inspection_done",  "field.building_inspection_done", true);
        addYesNoNaQuestion(fields, "flood_zone_checked",        "field.flood_zone_checked",       true);

        SectionTemplateJson template = new SectionTemplateJson();
        template.fields = fields;
        var st = sectionService.create("property-details", "section.property_details", template);
        sectionService.publish("property-details", st.getVersion());
    }

    // ── Proposal tab ──────────────────────────────────────────────────────────────

    private void seedProposalTab() {
        if (tabRepo.findByTabIdAndVersion("proposal", 1).isPresent()) return;
        var tt = tabService.create("proposal", "tab.proposal", List.of(
            new TabSectionRef("key-information",   1),
            new TabSectionRef("associated-records", 1),
            new TabSectionRef("guarantors",         1),
            new TabSectionRef("financial-details",  1),
            new TabSectionRef("compliance-aml",     1),
            new TabSectionRef("employment-income",  1),
            new TabSectionRef("property-details",   1)));
        tabService.publish("proposal", tt.getVersion());
    }

    // ── Shared field builders ─────────────────────────────────────────────────────

    /** Adds a Yes/No/N-A dropdown + conditional comment field (required + min 5 chars on NO). */
    private void addYesNoNaQuestion(List<FieldDefinition> fields,
                                     String questionKey, String questionLabelKey,
                                     boolean required) {
        FieldDefinition question = field(questionKey, FieldType.DROPDOWN, questionLabelKey,
            StorageDefinition.eav(), required);
        question.dropdownSource = DropdownSource.staticSource(YES_NO_NA);
        fields.add(question);

        String commentKey = questionKey + "_comment";
        FieldDefinition comment = field(commentKey, FieldType.TEXTAREA,
            questionLabelKey + "_comment", StorageDefinition.eav(), false);

        comment.visibilityRules = List.of(
            showWhen("show-" + commentKey, commentKey, questionKey, "NO"));

        ValidationRule reqOnNo = validationRule("req-" + commentKey, commentKey, ValidationType.REQUIRED);
        reqOnNo.condition = eqCondition(questionKey, "NO");

        ValidationRule minLenOnNo = validationRule("minlen-" + commentKey, commentKey, ValidationType.MIN_LENGTH);
        minLenOnNo.minLength = 5;
        minLenOnNo.condition = eqCondition(questionKey, "NO");

        comment.validationRules = List.of(reqOnNo, minLenOnNo);
        fields.add(comment);
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

    private ValidationRule validationRule(String ruleId, String targetFieldKey,
                                           ValidationType type) {
        ValidationRule r = new ValidationRule();
        r.ruleId = ruleId;
        r.scope = RuleScope.SECTION;
        r.targetFieldKey = targetFieldKey;
        r.validationType = type;
        return r;
    }

    private VisibilityRule showWhen(String ruleId, String targetFieldKey,
                                    String condFieldKey, String condValue) {
        VisibilityRule r = new VisibilityRule();
        r.ruleId = ruleId;
        r.scope = RuleScope.SECTION;
        r.targetFieldKey = targetFieldKey;
        r.show = true;
        r.condition = eqCondition(condFieldKey, condValue);
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
