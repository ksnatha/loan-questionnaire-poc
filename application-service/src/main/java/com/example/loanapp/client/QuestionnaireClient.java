package com.example.loanapp.client;

import com.example.common.template.SectionTemplateJson;
import com.example.common.template.TabTemplateJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class QuestionnaireClient {

    public record SectionData(String labelKey, SectionTemplateJson template) {}

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public QuestionnaireClient(
            @Value("${questionnaire.service.url:http://localhost:8082}") String baseUrl,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public TabTemplateJson getTabTemplate(String tabId, Integer version) {
        try {
            JsonNode resp = restClient.get()
                .uri("/tabs/{tabId}/versions/{version}", tabId, version)
                .retrieve()
                .body(JsonNode.class);
            return objectMapper.treeToValue(resp.get("template"), TabTemplateJson.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch tab template " + tabId + " v" + version, e);
        }
    }

    public SectionData getSectionData(String sectionId, Integer version) {
        try {
            JsonNode resp = restClient.get()
                .uri("/sections/{sectionId}/versions/{version}", sectionId, version)
                .retrieve()
                .body(JsonNode.class);
            String labelKey = resp.path("labelKey").asText(sectionId);
            SectionTemplateJson template =
                objectMapper.treeToValue(resp.get("template"), SectionTemplateJson.class);
            return new SectionData(labelKey, template);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch section " + sectionId + " v" + version, e);
        }
    }
}
