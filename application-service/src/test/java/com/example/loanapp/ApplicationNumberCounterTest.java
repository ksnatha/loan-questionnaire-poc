package com.example.loanapp;

import com.example.loanapp.client.LookupClient;
import com.example.loanapp.client.QuestionnaireClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationNumberCounterTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean QuestionnaireClient questionnaireClient;
    @MockBean LookupClient lookupClient;

    @Test
    void sequentialApplicationsGetUniqueSequentialIds() throws Exception {
        int year = LocalDate.now().getYear();
        Set<String> seenIds = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            String resp = mockMvc.perform(post("/api/applications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"createdUser\":\"countertest\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.humanReadableId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

            String hrid = objectMapper.readTree(resp).get("humanReadableId").asText();
            assertTrue(seenIds.add(hrid), "Duplicate human-readable ID: " + hrid);
            assertTrue(hrid.startsWith("L" + year + "-"),
                "ID must start with L<year>-: " + hrid);
        }
    }
}
