package com.example.loanapp;

import com.example.common.template.*;
import com.example.loanapp.client.LookupClient;
import com.example.loanapp.client.QuestionnaireClient;
import com.example.loanapp.client.QuestionnaireClient.SectionData;
import com.example.loanapp.entity.*;
import com.example.loanapp.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice F — Three independent versioning axes proven as automated tests.
 * Each test restores the ACTIVE snapshot in @AfterEach so tests are isolated.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VersioningIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ConfigSnapshotRepository snapshotRepo;
    @Autowired ConfigSnapshotItemRepository itemRepo;
    @Autowired LoanApplicationRepository appRepo;
    @MockBean QuestionnaireClient questionnaireClient;
    @MockBean LookupClient lookupClient;

    private Long originalActiveSnapshotId;

    @BeforeEach
    void recordActiveSnapshot() {
        originalActiveSnapshotId = snapshotRepo.findByStatus(SnapshotStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("No ACTIVE snapshot at test start"))
            .getId();
    }

    @AfterEach
    void restoreActiveSnapshot() {
        if (originalActiveSnapshotId == null) return;
        // Retire any snapshots that became ACTIVE during the test
        snapshotRepo.findAll().stream()
            .filter(s -> s.getStatus() == SnapshotStatus.ACTIVE
                      && !s.getId().equals(originalActiveSnapshotId))
            .forEach(s -> { s.setStatus(SnapshotStatus.RETIRED); snapshotRepo.save(s); });
        // Re-activate the original
        snapshotRepo.findById(originalActiveSnapshotId).ifPresent(s -> {
            if (s.getStatus() != SnapshotStatus.ACTIVE) {
                s.setStatus(SnapshotStatus.ACTIVE);
                snapshotRepo.save(s);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Axis 1: Template versioning — in-flight loans keep the old section version
    // -------------------------------------------------------------------------
    @Test
    void axis1_inFlightLoanPinnedToOldSectionVersion() throws Exception {
        TabTemplateJson tab = singleSectionTab("key-information");
        when(questionnaireClient.getTabTemplate(eq("proposal"), anyInt())).thenReturn(tab);
        when(questionnaireClient.getSectionData(eq("key-information"), eq(1)))
            .thenReturn(new SectionData("section.key_information", sectionV1()));  // 3 fields
        when(questionnaireClient.getSectionData(eq("key-information"), eq(2)))
            .thenReturn(new SectionData("section.key_information", sectionV2()));  // 4 fields

        // Loan A — created under original snapshot (section v1)
        long loanA = createApplication();

        // Publish key-information v2: clone-forward, override section version
        mockMvc.perform(post("/snapshots/clone-forward")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "newSnapshotCode": "SNAPSHOT-AXIS1-V2",
                      "overrides": {
                        "SECTION_KEY_INFORMATION": { "versionValue": 2 }
                      }
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Loan B — created under the new snapshot (section v2)
        long loanB = createApplication();

        // Loan A renders the OLD section — still 3 fields (pinned snapshot)
        mockMvc.perform(get("/api/applications/{id}/render", loanA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tab.sections[0].fields.length()").value(3));

        // Loan B renders the NEW section — 4 fields
        mockMvc.perform(get("/api/applications/{id}/render", loanB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tab.sections[0].fields.length()").value(4));
    }

    // -------------------------------------------------------------------------
    // Axis 2: CODE_SET effective-dating — labels resolve as-of loan creation date
    // -------------------------------------------------------------------------
    @Test
    void axis2_codeSetLabelResolvesAsOfCreationDate() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();

        TabTemplateJson tab = singleSectionTab("key-information");
        when(questionnaireClient.getTabTemplate(eq("proposal"), anyInt())).thenReturn(tab);
        when(questionnaireClient.getSectionData(eq("key-information"), anyInt()))
            .thenReturn(new SectionData("section.key_information", sectionWithLoanPurpose()));

        // The LOAN_PURPOSE code set: old label active yesterday, new label active today
        when(lookupClient.getCodeSet(eq("LOAN_PURPOSE"), eq(yesterday)))
            .thenReturn(List.of(new FieldOption("REFI", "Refinance")));
        when(lookupClient.getCodeSet(eq("LOAN_PURPOSE"), eq(today)))
            .thenReturn(List.of(new FieldOption("REFI", "Refi (Updated)")));

        // Loan A: created, then backdated to yesterday
        long loanA = createApplication();
        LoanApplication appA = appRepo.findById(loanA).orElseThrow();
        appA.setCreatedDate(yesterday);
        appRepo.save(appA);

        // Loan B: created today (default)
        long loanB = createApplication();

        // Loan A renders old "Refinance" label (asOf = yesterday)
        mockMvc.perform(get("/api/applications/{id}/render", loanA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tab.sections[0].fields[0].resolvedOptions[0].labelKey")
                .value("Refinance"));

        // Loan B renders new "Refi (Updated)" label (asOf = today)
        mockMvc.perform(get("/api/applications/{id}/render", loanB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tab.sections[0].fields[0].resolvedOptions[0].labelKey")
                .value("Refi (Updated)"));
    }

    // -------------------------------------------------------------------------
    // Axis 3: Rating logic versioning — ratingV2 factors in collateral grid count
    // -------------------------------------------------------------------------
    @Test
    void axis3_inFlightLoanKeepsOldRatingStrategy() throws Exception {
        TabTemplateJson tab = singleSectionTab("key-information");
        when(questionnaireClient.getTabTemplate(eq("proposal"), anyInt())).thenReturn(tab);
        when(questionnaireClient.getSectionData(eq("key-information"), anyInt()))
            .thenReturn(new SectionData("section.key_information", sectionWithGrid()));

        // Loan A — created under original snapshot (ratingV1)
        long loanA = createApplication();

        // Upgrade rating logic to ratingV2 via clone-forward
        mockMvc.perform(post("/snapshots/clone-forward")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "newSnapshotCode": "SNAPSHOT-AXIS3-V2",
                      "overrides": {
                        "RATING_LOGIC": { "versionValue": 2, "strategyBeanName": "ratingV2" }
                      }
                    }
                    """))
            .andExpect(status().isCreated());

        // Loan C — created under new snapshot (ratingV2)
        long loanC = createApplication();

        // Both loans: amount=2,500,000 (HIGH base) with 2 collateral properties
        String twoCollateralRows = """
            {
              "answers": { "loan_amount": "2500000" },
              "gridAnswers": {
                "collateral_properties": [
                  {"property_address": "1 Main St", "property_type": "RESIDENTIAL"},
                  {"property_address": "2 Oak Ave", "property_type": "COMMERCIAL"}
                ]
              }
            }
            """;

        // Loan A — ratingV1 ignores collateral_count → HIGH
        mockMvc.perform(put("/api/applications/{id}/draft", loanA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(twoCollateralRows))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.riskRating").value("HIGH"));

        // Loan C — ratingV2 sees collateral_count=2 → HIGH reduced to MEDIUM
        mockMvc.perform(put("/api/applications/{id}/draft", loanC)
                .contentType(MediaType.APPLICATION_JSON)
                .content(twoCollateralRows))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.riskRating").value("MEDIUM"));
    }

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private long createApplication() throws Exception {
        String resp = mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"createdUser\":\"test\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("id").asLong();
    }

    private TabTemplateJson singleSectionTab(String sectionId) {
        TabTemplateJson tab = new TabTemplateJson();
        tab.tabId = "proposal";
        tab.labelKey = "tab.proposal";
        tab.sections = List.of(new TabSectionRef(sectionId, 1));
        return tab;
    }

    private SectionTemplateJson sectionV1() {
        SectionTemplateJson s = new SectionTemplateJson();
        s.fields = new ArrayList<>(List.of(
            dedicatedField("proposal_name",        FieldType.TEXT,     "loan_application", "proposal_name",        true),
            dedicatedField("loan_amount",           FieldType.NUMBER,   "loan_application", "loan_amount",          true),
            dedicatedField("proposal_description",  FieldType.TEXTAREA, "loan_application", "proposal_description", false)
        ));
        return s;
    }

    private SectionTemplateJson sectionV2() {
        // v2 adds a new EAV field "annual_revenue" on top of v1's 3 dedicated fields
        SectionTemplateJson s = sectionV1();
        s.fields.add(eavField("annual_revenue", FieldType.NUMBER, false));
        return s;
    }

    private SectionTemplateJson sectionWithLoanPurpose() {
        SectionTemplateJson s = new SectionTemplateJson();
        FieldDefinition f = eavField("loan_purpose", FieldType.DROPDOWN, false);
        f.dropdownSource = DropdownSource.codeSet("LOAN_PURPOSE");
        s.fields = new ArrayList<>(List.of(f));
        return s;
    }

    private SectionTemplateJson sectionWithGrid() {
        SectionTemplateJson s = sectionV1();
        GridDefinition grid = new GridDefinition();
        grid.gridKey = "collateral_properties";
        grid.labelKey = "grid.collateral_properties";
        grid.columns = new ArrayList<>(List.of(
            eavField("property_address", FieldType.TEXT,     false),
            eavField("property_type",    FieldType.DROPDOWN, false)
        ));
        s.grids = new ArrayList<>(List.of(grid));
        return s;
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
