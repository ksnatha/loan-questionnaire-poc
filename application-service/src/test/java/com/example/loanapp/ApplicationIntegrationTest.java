package com.example.loanapp;

import com.example.common.template.*;
import com.example.loanapp.client.LookupClient;
import com.example.loanapp.client.QuestionnaireClient;
import com.example.loanapp.client.QuestionnaireClient.SectionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean QuestionnaireClient questionnaireClient;
    @MockBean LookupClient lookupClient;

    @BeforeEach
    void setupMocks() {
        when(questionnaireClient.getTabTemplate(eq("proposal"), anyInt()))
            .thenReturn(buildTab());
        when(questionnaireClient.getSectionData(eq("key-information"), anyInt()))
            .thenReturn(new SectionData("section.key_information", buildKeyInfoSection()));
    }

    @Test
    void createSaveDraftSubmitFlow() throws Exception {
        // Create
        String createResp = mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"createdUser\":\"testuser\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.humanReadableId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(createResp).get("id").asLong();

        // Render — 3 fields in section
        mockMvc.perform(get("/api/applications/{id}/render", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tab.sections[0].fields.length()").value(3))
            .andExpect(jsonPath("$.tab.tabId").value("proposal"));

        // Save draft with all 3 DEDICATED fields — loan_amount=1,500,000 → base MEDIUM, no collateral field → no bump → MEDIUM
        String saveBody = """
            {
              "answers": {
                "proposal_name": "Acme Expansion",
                "loan_amount": "1500000",
                "proposal_description": "Term loan for expansion"
              }
            }
            """;
        mockMvc.perform(put("/api/applications/{id}/draft", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.riskRating").value("MEDIUM"));

        // Render again — answers persisted
        mockMvc.perform(get("/api/applications/{id}/render", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answers.proposal_name").value("Acme Expansion"))
            .andExpect(jsonPath("$.answers.loan_amount").value("1500000"));

        // Submit
        mockMvc.perform(post("/api/applications/{id}/submit", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void submitWithMissingRequiredFieldsReturns422() throws Exception {
        String createResp = mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"createdUser\":\"testuser\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(createResp).get("id").asLong();

        mockMvc.perform(post("/api/applications/{id}/submit", id))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void serverPurgesHiddenFieldEvenIfClientSendsIt() throws Exception {
        when(questionnaireClient.getSectionData(eq("key-information"), anyInt()))
            .thenReturn(new SectionData("section.key_information", buildSectionWithVisibilityRule()));

        String createResp = mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"createdUser\":\"testuser\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createResp).get("id").asLong();

        // Client sends existing_relationship_id even though has_existing_relationship = NO
        mockMvc.perform(put("/api/applications/{id}/draft", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "answers": {
                        "has_existing_relationship": "NO",
                        "existing_relationship_id": "BANK-12345"
                      }
                    }
                    """))
            .andExpect(status().isOk());

        // Server must have purged existing_relationship_id because it was hidden
        mockMvc.perform(get("/api/applications/{id}/render", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answers.has_existing_relationship").value("NO"))
            .andExpect(jsonPath("$.answers.existing_relationship_id").doesNotExist());
    }

    @Test
    void savesAndRendersEavFieldAlongsideDedicated() throws Exception {
        when(questionnaireClient.getTabTemplate(eq("proposal"), anyInt()))
            .thenReturn(buildTab());
        when(questionnaireClient.getSectionData(eq("key-information"), anyInt()))
            .thenReturn(new SectionData("section.key_information", buildMixedSection()));

        String createResp = mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"createdUser\":\"testuser\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(createResp).get("id").asLong();

        String saveBody = """
            {
              "answers": {
                "proposal_name": "Mixed Test",
                "has_existing_relationship": "YES"
              }
            }
            """;
        mockMvc.perform(put("/api/applications/{id}/draft", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/applications/{id}/render", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answers.proposal_name").value("Mixed Test"))
            .andExpect(jsonPath("$.answers.has_existing_relationship").value("YES"));
    }

    @Test
    void addAndEditLoanParty() throws Exception {
        // Create application
        String createResp = mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"createdUser\":\"testuser\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long appId = objectMapper.readTree(createResp).get("id").asLong();

        // Add a borrower
        String addPartyBody = """
            {
              "role": "BORROWER",
              "firstName": "Alice",
              "lastName": "Smith",
              "email": "alice@example.com"
            }
            """;
        String partyResp = mockMvc.perform(post("/api/applications/{id}/parties", appId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addPartyBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.role").value("BORROWER"))
            .andExpect(jsonPath("$.firstName").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andReturn().getResponse().getContentAsString();

        long partyId = objectMapper.readTree(partyResp).get("id").asLong();

        // Edit the party
        String editBody = """
            {
              "role": "BORROWER",
              "firstName": "Alice",
              "lastName": "Smith",
              "email": "alice.updated@example.com"
            }
            """;
        mockMvc.perform(put("/api/applications/{appId}/parties/{partyId}", appId, partyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(editBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice.updated@example.com"));

        // List parties — should see the updated borrower
        mockMvc.perform(get("/api/applications/{id}/parties", appId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].email").value("alice.updated@example.com"));
    }

    private TabTemplateJson buildTab() {
        TabTemplateJson tab = new TabTemplateJson();
        tab.tabId = "proposal";
        tab.labelKey = "tab.proposal";
        tab.sections = List.of(new TabSectionRef("key-information", 1));
        return tab;
    }

    private SectionTemplateJson buildKeyInfoSection() {
        SectionTemplateJson section = new SectionTemplateJson();
        section.fields = new ArrayList<>(List.of(
            dedicatedField("proposal_name", FieldType.TEXT, "loan_application", "proposal_name", true),
            dedicatedField("loan_amount", FieldType.NUMBER, "loan_application", "loan_amount", true),
            dedicatedField("proposal_description", FieldType.TEXTAREA,
                           "loan_application", "proposal_description", false)));
        return section;
    }

    @Test
    void gridRowsPersistAndReloadWithRowIdentity() throws Exception {
        TabTemplateJson tabWithGrid = buildTab();
        tabWithGrid.sections = List.of(new TabSectionRef("associated-records", 1));
        when(questionnaireClient.getTabTemplate(eq("proposal"), anyInt()))
            .thenReturn(tabWithGrid);
        when(questionnaireClient.getSectionData(eq("associated-records"), anyInt()))
            .thenReturn(new SectionData("section.associated_records", buildGridSection()));

        String createResp = mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"createdUser\":\"testuser\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createResp).get("id").asLong();

        // Save 2 rows
        mockMvc.perform(put("/api/applications/{id}/draft", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "answers": {},
                      "gridAnswers": {
                        "collateral_properties": [
                          {"property_address": "123 Main St", "property_type": "RESIDENTIAL"},
                          {"property_address": "456 Oak Ave", "property_type": "COMMERCIAL"}
                        ]
                      }
                    }
                    """))
            .andExpect(status().isOk());

        // Render — verify both rows with correct values
        mockMvc.perform(get("/api/applications/{id}/render", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gridAnswers.collateral_properties.length()").value(2))
            .andExpect(jsonPath("$.gridAnswers.collateral_properties[0].property_address").value("123 Main St"))
            .andExpect(jsonPath("$.gridAnswers.collateral_properties[1].property_address").value("456 Oak Ave"))
            .andExpect(jsonPath("$.gridAnswers.collateral_properties[1].property_type").value("COMMERCIAL"));

        // Save with 1 row — old rows should be replaced
        mockMvc.perform(put("/api/applications/{id}/draft", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "answers": {},
                      "gridAnswers": {
                        "collateral_properties": [
                          {"property_address": "789 Pine Rd", "property_type": "INDUSTRIAL"}
                        ]
                      }
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/applications/{id}/render", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gridAnswers.collateral_properties.length()").value(1))
            .andExpect(jsonPath("$.gridAnswers.collateral_properties[0].property_address").value("789 Pine Rd"));
    }

    private SectionTemplateJson buildGridSection() {
        SectionTemplateJson section = new SectionTemplateJson();
        GridDefinition grid = new GridDefinition();
        grid.gridKey = "collateral_properties";
        grid.labelKey = "grid.collateral_properties";
        grid.columns = new ArrayList<>(List.of(
            eavField("property_address", FieldType.TEXT, false),
            eavField("property_type", FieldType.DROPDOWN, false)));
        section.grids = new ArrayList<>(List.of(grid));
        return section;
    }

    private SectionTemplateJson buildSectionWithVisibilityRule() {
        SectionTemplateJson section = new SectionTemplateJson();
        FieldDefinition hasRel = eavField("has_existing_relationship", FieldType.DROPDOWN, false);

        FieldDefinition relId = eavField("existing_relationship_id", FieldType.TEXT, false);
        VisibilityRule showRule = new VisibilityRule();
        showRule.ruleId = "show-rel-id";
        showRule.scope = RuleScope.SECTION;
        showRule.show = true;
        RuleCondition cond = new RuleCondition();
        cond.fieldKey = "has_existing_relationship";
        cond.operator = RuleOperator.EQUALS;
        cond.value = "YES";
        showRule.condition = cond;
        showRule.targetFieldKey = "existing_relationship_id";
        relId.visibilityRules = List.of(showRule);

        section.fields = new ArrayList<>(List.of(hasRel, relId));
        return section;
    }

    private SectionTemplateJson buildMixedSection() {
        SectionTemplateJson section = new SectionTemplateJson();
        section.fields = new ArrayList<>(List.of(
            dedicatedField("proposal_name", FieldType.TEXT, "loan_application", "proposal_name", true),
            eavField("has_existing_relationship", FieldType.DROPDOWN, false)));
        return section;
    }

    private FieldDefinition eavField(String key, FieldType type, boolean required) {
        FieldDefinition f = new FieldDefinition();
        f.fieldKey = key;
        f.fieldType = type;
        f.labelKey = "field." + key;
        f.storage = StorageDefinition.eav();
        f.required = required;
        return f;
    }

    private FieldDefinition dedicatedField(String key, FieldType type,
                                            String table, String col, boolean required) {
        FieldDefinition f = new FieldDefinition();
        f.fieldKey = key;
        f.fieldType = type;
        f.labelKey = "field." + key;
        f.storage = StorageDefinition.dedicated(table, col);
        f.required = required;
        return f;
    }
}
