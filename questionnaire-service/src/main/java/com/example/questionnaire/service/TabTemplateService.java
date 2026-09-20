package com.example.questionnaire.service;

import com.example.common.template.*;
import com.example.questionnaire.dto.TabTemplateResponse;
import com.example.questionnaire.entity.*;
import com.example.questionnaire.repository.TabTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class TabTemplateService {

    private final TabTemplateRepository repo;
    private final ObjectMapper objectMapper;

    public TabTemplateService(TabTemplateRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    public TabTemplate create(String tabId, String labelKey, List<TabSectionRef> sections) {
        if (repo.findByTabIdAndVersion(tabId, 1).isPresent()) {
            throw new IllegalStateException("Tab template already exists: " + tabId + " v1");
        }
        try {
            TabTemplateJson json = new TabTemplateJson();
            json.tabId = tabId;
            json.labelKey = labelKey;
            json.sections = sections;
            TabTemplate tt = new TabTemplate();
            tt.setTabId(tabId);
            tt.setVersion(1);
            tt.setLabelKey(labelKey);
            tt.setTemplateJson(objectMapper.writeValueAsString(json));
            return repo.save(tt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize tab template", e);
        }
    }

    public TabTemplate publish(String tabId, Integer version) {
        TabTemplate tt = repo.findByTabIdAndVersion(tabId, version)
            .orElseThrow(() -> new NoSuchElementException("Not found: " + tabId + " v" + version));
        if (tt.getStatus() != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Tab template is not in DRAFT status");
        }
        tt.setStatus(TemplateStatus.ACTIVE);
        return repo.save(tt);
    }

    @Transactional(readOnly = true)
    public TabTemplateResponse getResponse(String tabId, Integer version) {
        TabTemplate tt = repo.findByTabIdAndVersion(tabId, version)
            .orElseThrow(() -> new NoSuchElementException("Not found: " + tabId + " v" + version));
        return toResponse(tt);
    }

    public TabTemplateResponse toResponse(TabTemplate tt) {
        try {
            TabTemplateResponse r = new TabTemplateResponse();
            r.tabId = tt.getTabId();
            r.version = tt.getVersion();
            r.status = tt.getStatus().name();
            r.labelKey = tt.getLabelKey();
            r.template = objectMapper.readValue(tt.getTemplateJson(), TabTemplateJson.class);
            return r;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize tab template", e);
        }
    }
}
