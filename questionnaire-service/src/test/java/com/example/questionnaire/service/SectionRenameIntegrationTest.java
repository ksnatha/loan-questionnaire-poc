package com.example.questionnaire.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 3 acceptance: change a section's label through the API, save as draft, publish —
 * confirm the new label is reflected without touching H2 directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SectionRenameIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void renameViaUpdateDraftPersistsThroughPublish() throws Exception {
        MvcResult activeResult = mockMvc.perform(get("/sections/guarantors/versions/1"))
            .andExpect(status().isOk())
            .andReturn();
        var active = objectMapper.readValue(
            activeResult.getResponse().getContentAsString(),
            com.example.questionnaire.dto.SectionTemplateResponse.class);

        String draftBody = objectMapper.writeValueAsString(
            java.util.Map.of("template", active.template));
        MvcResult draftResult = mockMvc.perform(post("/sections/guarantors/draft-revision")
                .contentType(MediaType.APPLICATION_JSON).content(draftBody))
            .andExpect(status().isCreated())
            .andReturn();
        int draftVersion = objectMapper.readTree(draftResult.getResponse().getContentAsString())
            .get("version").asInt();

        String renameBody = objectMapper.writeValueAsString(
            java.util.Map.of("template", active.template, "labelKey", "section.guarantors_renamed"));
        mockMvc.perform(put("/sections/guarantors/versions/" + draftVersion)
                .contentType(MediaType.APPLICATION_JSON).content(renameBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labelKey").value("section.guarantors_renamed"));

        mockMvc.perform(post("/sections/guarantors/versions/" + draftVersion + "/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labelKey").value("section.guarantors_renamed"));

        mockMvc.perform(get("/sections/guarantors/versions/" + draftVersion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.labelKey").value("section.guarantors_renamed"));

        mockMvc.perform(get("/sections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.sectionId == 'guarantors')].labelKey")
                .value(org.hamcrest.Matchers.contains("section.guarantors_renamed")));
    }
}
