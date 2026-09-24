package com.example.questionnaire.service;

import com.example.common.template.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuestionnaireFieldsBulkUploadIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private static String csv(String... rows) {
        return String.join("\n", rows) + "\n";
    }

    private static String row(String... cols) {
        return String.join(",", cols);
    }

    @Test
    @Order(1)
    void createsDraftFromFileWhenNoneExists() throws Exception {
        String header = row("row_type", "field_key", "label_key", "field_type", "storage_type",
            "dropdown_source_type", "dropdown_code_set_type", "static_options", "required", "display_order");
        String content = csv(header,
            row("FIELD", "compliance_note_a", "field.compliance_note_a", "TEXT", "EAV", "", "", "", "N", "1"),
            row("FIELD", "compliance_note_b", "field.compliance_note_b", "TEXT", "EAV", "", "", "", "N", "2"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "fields.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/sections/compliance-aml/bulk-upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sectionId").value("compliance-aml"))
            .andExpect(jsonPath("$.fieldCount").value(2))
            .andExpect(jsonPath("$.gridCount").value(0))
            .andReturn();

        int draftVersion = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("draftVersion").asInt();

        mockMvc.perform(get("/sections/compliance-aml/versions/" + draftVersion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.template.fields.length()").value(2))
            .andExpect(jsonPath("$.template.fields[0].fieldKey").value("compliance_note_a"))
            .andExpect(jsonPath("$.template.fields[1].fieldKey").value("compliance_note_b"));
    }

    @Test
    @Order(2)
    void rejectsDedicatedFieldNotInAllowlist() throws Exception {
        String header = row("row_type", "field_key", "label_key", "field_type", "storage_type",
            "dedicated_table", "dedicated_column", "required", "display_order");
        String content = csv(header,
            row("FIELD", "bogus_field", "field.bogus_field", "TEXT", "DEDICATED",
                "not_a_real_table", "not_a_real_column", "N", "1"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "fields.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/sections/guarantors/bulk-upload").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", org.hamcrest.Matchers.containsString(
                "not_a_real_table.not_a_real_column")));
    }

    @Test
    @Order(3)
    void preservesConditionalRulesForUnchangedFieldsOnFullReplaceUpload() throws Exception {
        // Reproduce financial-details' 15 seeded fields, changing only primary_bank's label.
        // visibilityRules/validationRules aren't upload columns at all — a correct merge must
        // carry them forward for every fieldKey that already existed, not just the untouched ones.
        String header = row("row_type", "field_key", "label_key", "field_type", "storage_type",
            "dedicated_table", "dedicated_column",
            "dropdown_source_type", "dropdown_code_set_type", "static_options",
            "required", "display_order");
        String content = csv(header,
            row("FIELD", "net_worth", "field.net_worth", "NUMBER", "DEDICATED",
                "loan_financial_summary", "net_worth", "", "", "", "Y", "1"),
            row("FIELD", "annual_revenue", "field.annual_revenue", "NUMBER", "DEDICATED",
                "loan_financial_summary", "annual_revenue", "", "", "", "Y", "2"),
            row("FIELD", "total_liabilities", "field.total_liabilities", "NUMBER", "DEDICATED",
                "loan_financial_summary", "total_liabilities", "", "", "", "Y", "3"),
            row("FIELD", "credit_score", "field.credit_score", "NUMBER", "DEDICATED",
                "loan_financial_summary", "credit_score", "", "", "", "N", "4"),
            row("FIELD", "primary_bank", "field.primary_bank_updated", "TEXT", "EAV",
                "", "", "", "", "", "N", "5"),
            row("FIELD", "years_in_business", "field.years_in_business", "NUMBER", "EAV",
                "", "", "", "", "", "Y", "6"),
            row("FIELD", "existing_loan_count", "field.existing_loan_count", "NUMBER", "EAV",
                "", "", "", "", "", "N", "7"),
            row("FIELD", "fiscal_year_end", "field.fiscal_year_end", "DROPDOWN", "EAV",
                "", "", "CODE_SET", "FISCAL_YEAR_END", "", "N", "8"),
            row("FIELD", "business_structure", "field.business_structure", "DROPDOWN", "EAV",
                "", "", "CODE_SET", "BUSINESS_STRUCTURE", "", "N", "9"),
            row("FIELD", "financial_statements_provided", "field.financial_statements_provided",
                "DROPDOWN", "EAV", "", "", "STATIC", "", "YES:Yes;NO:No;NA:N/A", "Y", "10"),
            row("FIELD", "financial_statements_provided_comment",
                "field.financial_statements_provided_comment", "TEXTAREA", "EAV",
                "", "", "", "", "", "N", "11"),
            row("FIELD", "credit_threshold_met", "field.credit_threshold_met", "DROPDOWN", "EAV",
                "", "", "STATIC", "", "YES:Yes;NO:No;NA:N/A", "Y", "12"),
            row("FIELD", "credit_threshold_met_comment", "field.credit_threshold_met_comment",
                "TEXTAREA", "EAV", "", "", "", "", "", "N", "13"),
            row("FIELD", "liabilities_disclosed", "field.liabilities_disclosed", "DROPDOWN", "EAV",
                "", "", "STATIC", "", "YES:Yes;NO:No;NA:N/A", "Y", "14"),
            row("FIELD", "liabilities_disclosed_comment", "field.liabilities_disclosed_comment",
                "TEXTAREA", "EAV", "", "", "", "", "", "N", "15"));

        MockMultipartFile file = new MockMultipartFile(
            "file", "fields.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/sections/financial-details/bulk-upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fieldCount").value(15))
            .andReturn();

        int draftVersion = objectMapper.readTree(result.getResponse().getContentAsString())
            .get("draftVersion").asInt();

        MvcResult sectionResult = mockMvc.perform(
                get("/sections/financial-details/versions/" + draftVersion))
            .andExpect(status().isOk())
            .andReturn();

        var response = objectMapper.readValue(
            sectionResult.getResponse().getContentAsString(),
            com.example.questionnaire.dto.SectionTemplateResponse.class);

        FieldDefinition primaryBank = fieldByKey(response.template.fields, "primary_bank");
        assertEquals("field.primary_bank_updated", primaryBank.labelKey);

        for (String commentKey : List.of("financial_statements_provided_comment",
                                          "credit_threshold_met_comment",
                                          "liabilities_disclosed_comment")) {
            FieldDefinition comment = fieldByKey(response.template.fields, commentKey);
            assertFalse(comment.visibilityRules.isEmpty(),
                commentKey + " lost its visibilityRules on upload");
            assertFalse(comment.validationRules.isEmpty(),
                commentKey + " lost its validationRules on upload");
        }
    }

    private FieldDefinition fieldByKey(List<FieldDefinition> fields, String key) {
        return fields.stream().filter(f -> key.equals(f.fieldKey)).findFirst()
            .orElseThrow(() -> new AssertionError("Field not found: " + key));
    }
}
