package com.example.loanapp.client;

import com.example.common.template.FieldOption;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class LookupClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LookupClient(
            @Value("${lookup.service.url:http://localhost:8081}") String baseUrl,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public List<FieldOption> getCodeSet(String codeSetType, LocalDate asOf) {
        try {
            JsonNode arr = restClient.get()
                .uri("/code-sets/{type}?asOf={asOf}", codeSetType, asOf.toString())
                .retrieve()
                .body(JsonNode.class);
            return toFieldOptions(arr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch code set " + codeSetType, e);
        }
    }

    public List<FieldOption> getLookup(String sourceKey) {
        try {
            JsonNode arr = restClient.get()
                .uri("/lookups/{sourceKey}", sourceKey)
                .retrieve()
                .body(JsonNode.class);
            return toFieldOptions(arr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch lookup " + sourceKey, e);
        }
    }

    private List<FieldOption> toFieldOptions(JsonNode arr) {
        List<FieldOption> options = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode node : arr) {
                options.add(new FieldOption(
                    node.path("code").asText(),
                    node.path("label").asText()));
            }
        }
        return options;
    }
}
