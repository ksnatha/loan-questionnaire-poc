package com.example.questionnaire.service;

import com.example.common.template.*;
import com.example.questionnaire.dto.TabTemplateResponse;
import com.example.questionnaire.entity.*;
import com.example.questionnaire.repository.TabTemplateSectionRepository;
import com.example.questionnaire.repository.TabTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TabTemplateService {

    private final TabTemplateRepository repo;
    private final TabTemplateSectionRepository sectionRepo;
    private final ObjectMapper objectMapper;

    public TabTemplateService(TabTemplateRepository repo,
                               TabTemplateSectionRepository sectionRepo,
                               ObjectMapper objectMapper) {
        this.repo = repo;
        this.sectionRepo = sectionRepo;
        this.objectMapper = objectMapper;
    }

    public TabTemplate create(String tabId, String labelKey, List<TabSectionRef> sections) {
        if (repo.findByTabIdAndVersion(tabId, 1).isPresent()) {
            throw new IllegalStateException("Tab template already exists: " + tabId + " v1");
        }
        TabTemplate tt = new TabTemplate();
        tt.setTabId(tabId);
        tt.setVersion(1);
        tt.setLabelKey(labelKey);
        tt.setTemplateJson("{}");
        tt = repo.save(tt);
        persistSectionRefs(tt.getId(), sections);
        return tt;
    }

    public TabTemplate publish(String tabId, Integer version) {
        TabTemplate tt = repo.findByTabIdAndVersion(tabId, version)
            .orElseThrow(() -> new NoSuchElementException("Not found: " + tabId + " v" + version));
        if (tt.getStatus() != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Tab template is not in DRAFT status");
        }
        // Close out the previous ACTIVE version's effective date range.
        // Status stays ACTIVE so snapshot-pinned loans can still fetch it by version.
        repo.findByTabIdAndStatus(tabId, TemplateStatus.ACTIVE).forEach(t -> {
            t.setEffectiveEnd(LocalDate.now().minusDays(1));
            repo.save(t);
        });
        tt.setStatus(TemplateStatus.ACTIVE);
        tt.setEffectiveStart(LocalDate.now());
        return repo.save(tt);
    }

    @Transactional(readOnly = true)
    public TabTemplateResponse getResponse(String tabId, Integer version) {
        TabTemplate tt = repo.findByTabIdAndVersion(tabId, version)
            .orElseThrow(() -> new NoSuchElementException("Not found: " + tabId + " v" + version));
        return toResponse(tt);
    }

    public TabTemplateResponse toResponse(TabTemplate tt) {
        List<TabTemplateSection> rows = sectionRepo.findByTabTemplateIdOrderByDisplayOrder(tt.getId());
        List<TabSectionRef> sectionRefs = rows.stream()
            .map(r -> new TabSectionRef(r.getSectionId(), r.getDisplayOrder()))
            .collect(Collectors.toList());

        TabTemplateJson json = new TabTemplateJson();
        json.tabId = tt.getTabId();
        json.labelKey = tt.getLabelKey();
        json.sections = sectionRefs;

        TabTemplateResponse r = new TabTemplateResponse();
        r.tabId = tt.getTabId();
        r.version = tt.getVersion();
        r.status = tt.getStatus().name();
        r.labelKey = tt.getLabelKey();
        r.template = json;
        return r;
    }

    private void persistSectionRefs(Long tabTemplateId, List<TabSectionRef> sections) {
        sectionRepo.deleteByTabTemplateId(tabTemplateId);
        for (TabSectionRef ref : sections) {
            TabTemplateSection row = new TabTemplateSection();
            row.setTabTemplateId(tabTemplateId);
            row.setSectionId(ref.sectionId);
            row.setDisplayOrder(ref.displayOrder);
            sectionRepo.save(row);
        }
    }
}
