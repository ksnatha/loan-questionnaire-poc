package com.example.questionnaire.service;

import com.example.common.template.*;
import com.example.questionnaire.dto.SectionTemplateResponse;
import com.example.questionnaire.entity.*;
import com.example.questionnaire.repository.SectionTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        st.setStatus(TemplateStatus.ACTIVE);
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
