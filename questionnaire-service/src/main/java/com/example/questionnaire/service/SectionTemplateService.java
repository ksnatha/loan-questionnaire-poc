package com.example.questionnaire.service;

import com.example.common.template.*;
import com.example.questionnaire.dto.SectionListItem;
import com.example.questionnaire.dto.SectionTemplateResponse;
import com.example.questionnaire.entity.*;
import com.example.questionnaire.repository.SectionTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SectionTemplateService {

    private final SectionTemplateRepository repo;
    private final AllowlistValidator validator;
    private final ObjectMapper objectMapper;

    public SectionTemplateService(SectionTemplateRepository repo,
                                   AllowlistValidator validator,
                                   ObjectMapper objectMapper) {
        this.repo = repo;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SectionListItem> listSections() {
        List<SectionTemplate> all = repo.findAll();
        Map<String, List<SectionTemplate>> bySection = all.stream()
            .collect(Collectors.groupingBy(SectionTemplate::getSectionId));

        List<SectionListItem> result = new ArrayList<>();
        for (Map.Entry<String, List<SectionTemplate>> entry : bySection.entrySet()) {
            SectionTemplate active = entry.getValue().stream()
                .filter(st -> st.getStatus() == TemplateStatus.ACTIVE)
                .max(Comparator.comparingInt(SectionTemplate::getVersion)).orElse(null);
            SectionTemplate draft = entry.getValue().stream()
                .filter(st -> st.getStatus() == TemplateStatus.DRAFT)
                .max(Comparator.comparingInt(SectionTemplate::getVersion)).orElse(null);

            SectionTemplate display = active != null ? active : draft;
            if (display == null) continue;

            SectionListItem item = new SectionListItem();
            item.sectionId = entry.getKey();
            item.labelKey = display.getLabelKey();
            item.activeVersion = active != null ? active.getVersion() : 0;
            item.draftVersion = draft != null ? draft.getVersion() : null;
            item.hasGrid = display.isHasGrid();
            try {
                SectionTemplateJson t = objectMapper.readValue(display.getTemplateJson(), SectionTemplateJson.class);
                item.fieldCount = t.fields != null ? t.fields.size() : 0;
            } catch (Exception ignored) {}
            result.add(item);
        }
        result.sort(Comparator.comparing(i -> i.sectionId));
        return result;
    }

    public SectionTemplate createDraftRevision(String sectionId, SectionTemplateJson newTemplate) {
        List<SectionTemplate> versions = repo.findBySectionIdOrderByVersionDesc(sectionId);
        if (versions.isEmpty()) throw new NoSuchElementException("Section not found: " + sectionId);

        versions.stream().filter(st -> st.getStatus() == TemplateStatus.DRAFT).findFirst()
            .ifPresent(d -> { throw new IllegalStateException(
                "Draft already exists: " + sectionId + " v" + d.getVersion()); });

        SectionTemplate latestActive = versions.stream()
            .filter(st -> st.getStatus() == TemplateStatus.ACTIVE).findFirst()
            .orElseThrow(() -> new IllegalStateException("No ACTIVE version for: " + sectionId));

        try {
            SectionTemplateJson template = newTemplate != null ? newTemplate
                : objectMapper.readValue(latestActive.getTemplateJson(), SectionTemplateJson.class);

            List<String> refs = template.fields == null ? List.of()
                : template.fields.stream()
                    .filter(f -> f.storage != null && f.storage.type == StorageType.DEDICATED)
                    .map(f -> f.storage.tableName + "." + f.storage.columnName)
                    .collect(Collectors.toList());

            SectionTemplate draft = new SectionTemplate();
            draft.setSectionId(sectionId);
            draft.setVersion(latestActive.getVersion() + 1);
            draft.setLabelKey(latestActive.getLabelKey());
            draft.setStatus(TemplateStatus.DRAFT);
            draft.setHasGrid(template.grids != null && !template.grids.isEmpty());
            draft.setDedicatedColumnRefs(objectMapper.writeValueAsString(refs));
            draft.setTemplateJson(objectMapper.writeValueAsString(template));
            return repo.save(draft);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create draft revision", e);
        }
    }

    public SectionTemplate updateDraft(String sectionId, Integer version, SectionTemplateJson newTemplate) {
        SectionTemplate st = repo.findBySectionIdAndVersion(sectionId, version)
            .orElseThrow(() -> new NoSuchElementException("Not found: " + sectionId + " v" + version));
        if (st.getStatus() != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Version " + version + " is not in DRAFT status");
        }
        try {
            List<String> refs = newTemplate.fields == null ? List.of()
                : newTemplate.fields.stream()
                    .filter(f -> f.storage != null && f.storage.type == StorageType.DEDICATED)
                    .map(f -> f.storage.tableName + "." + f.storage.columnName)
                    .collect(Collectors.toList());
            st.setHasGrid(newTemplate.grids != null && !newTemplate.grids.isEmpty());
            st.setDedicatedColumnRefs(objectMapper.writeValueAsString(refs));
            st.setTemplateJson(objectMapper.writeValueAsString(newTemplate));
            return repo.save(st);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update draft", e);
        }
    }

    public SectionTemplate create(String sectionId, String labelKey, SectionTemplateJson template) {
        if (repo.findBySectionIdAndVersion(sectionId, 1).isPresent()) {
            throw new IllegalStateException("Section template already exists: " + sectionId + " v1");
        }
        try {
            List<String> refs = template.fields.stream()
                .filter(f -> f.storage != null && f.storage.type == StorageType.DEDICATED)
                .map(f -> f.storage.tableName + "." + f.storage.columnName)
                .collect(Collectors.toList());
            SectionTemplate st = new SectionTemplate();
            st.setSectionId(sectionId);
            st.setVersion(1);
            st.setLabelKey(labelKey);
            st.setHasGrid(!template.grids.isEmpty());
            st.setDedicatedColumnRefs(objectMapper.writeValueAsString(refs));
            st.setTemplateJson(objectMapper.writeValueAsString(template));
            return repo.save(st);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize template", e);
        }
    }

    public SectionTemplate publish(String sectionId, Integer version) {
        SectionTemplate st = repo.findBySectionIdAndVersion(sectionId, version)
            .orElseThrow(() -> new NoSuchElementException("Not found: " + sectionId + " v" + version));
        if (st.getStatus() != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Section template is not in DRAFT status");
        }
        try {
            SectionTemplateJson template = objectMapper.readValue(st.getTemplateJson(), SectionTemplateJson.class);
            validator.validate(template);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse template for validation", e);
        }
        // Close out the previous ACTIVE version's effective date range.
        // Status stays ACTIVE so snapshot-pinned loans can still fetch it by version.
        repo.findBySectionIdOrderByVersionDesc(sectionId).stream()
            .filter(s -> s.getStatus() == TemplateStatus.ACTIVE)
            .forEach(s -> {
                s.setEffectiveEnd(LocalDate.now().minusDays(1));
                repo.save(s);
            });
        st.setStatus(TemplateStatus.ACTIVE);
        st.setEffectiveStart(LocalDate.now());
        return repo.save(st);
    }

    @Transactional(readOnly = true)
    public SectionTemplateResponse getResponse(String sectionId, Integer version) {
        SectionTemplate st = repo.findBySectionIdAndVersion(sectionId, version)
            .orElseThrow(() -> new NoSuchElementException("Not found: " + sectionId + " v" + version));
        return toResponse(st);
    }

    public SectionTemplateResponse toResponse(SectionTemplate st) {
        try {
            SectionTemplateResponse r = new SectionTemplateResponse();
            r.sectionId = st.getSectionId();
            r.version = st.getVersion();
            r.status = st.getStatus().name();
            r.labelKey = st.getLabelKey();
            r.hasGrid = st.isHasGrid();
            String refs = st.getDedicatedColumnRefs();
            r.dedicatedColumnRefs = objectMapper.readValue(
                refs != null ? refs : "[]", new TypeReference<List<String>>() {});
            r.template = objectMapper.readValue(st.getTemplateJson(), SectionTemplateJson.class);
            return r;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize template", e);
        }
    }
}
