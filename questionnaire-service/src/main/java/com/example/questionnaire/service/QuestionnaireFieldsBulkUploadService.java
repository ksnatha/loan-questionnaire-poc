package com.example.questionnaire.service;

import com.example.common.template.*;
import com.example.questionnaire.dto.BulkUploadFieldsResponse;
import com.example.questionnaire.entity.SectionTemplate;
import com.example.questionnaire.entity.TemplateStatus;
import com.example.questionnaire.repository.SectionTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses a flat FIELD/GRID_COLUMN spreadsheet into a {@link SectionTemplateJson} and saves it as
 * the section's DRAFT, reusing {@link SectionTemplateService}'s existing draft-lifecycle methods.
 * Field-level attributes and grid membership are fully replaced by the file's contents;
 * visibilityRules/validationRules (which the upload format has no columns for) are carried
 * forward unchanged for any fieldKey that already existed in the current draft/active template.
 */
@Service
public class QuestionnaireFieldsBulkUploadService {

    private final SectionTemplateRepository repo;
    private final SectionTemplateService templateService;
    private final AllowlistValidator validator;
    private final ObjectMapper objectMapper;

    public QuestionnaireFieldsBulkUploadService(SectionTemplateRepository repo,
                                                 SectionTemplateService templateService,
                                                 AllowlistValidator validator,
                                                 ObjectMapper objectMapper) {
        this.repo = repo;
        this.templateService = templateService;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BulkUploadFieldsResponse bulkUpload(String sectionId, MultipartFile file) {
        List<SectionTemplate> versions = repo.findBySectionIdOrderByVersionDesc(sectionId);
        if (versions.isEmpty()) {
            throw new NoSuchElementException("Section not found: " + sectionId);
        }
        SectionTemplate draft = versions.stream()
            .filter(v -> v.getStatus() == TemplateStatus.DRAFT).findFirst().orElse(null);
        SectionTemplate active = versions.stream()
            .filter(v -> v.getStatus() == TemplateStatus.ACTIVE).findFirst().orElse(null);
        SectionTemplate mergeSource = draft != null ? draft : active;
        if (mergeSource == null) {
            throw new IllegalStateException("No DRAFT or ACTIVE version for: " + sectionId);
        }

        SectionTemplateJson parsed = parseTemplate(file);
        mergeRulesForward(readTemplate(mergeSource), parsed);

        // Recommended addition: validate at upload time too (not just at publish), so a bad
        // DEDICATED reference fails immediately with the specific ref, not later at publish.
        validator.validate(parsed);

        SectionTemplate saved = draft != null
            ? templateService.updateDraft(sectionId, draft.getVersion(), parsed)
            : templateService.createDraftRevision(sectionId, parsed);

        return new BulkUploadFieldsResponse(
            sectionId, saved.getVersion(), parsed.fields.size(), parsed.grids.size());
    }

    private SectionTemplateJson readTemplate(SectionTemplate st) {
        try {
            return objectMapper.readValue(st.getTemplateJson(), SectionTemplateJson.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse existing template for merge", e);
        }
    }

    private void mergeRulesForward(SectionTemplateJson current, SectionTemplateJson parsed) {
        Map<String, FieldDefinition> existingByKey = new HashMap<>();
        for (FieldDefinition f : current.fields) existingByKey.put(f.fieldKey, f);
        for (GridDefinition g : current.grids) {
            for (FieldDefinition c : g.columns) existingByKey.put(c.fieldKey, c);
        }

        for (FieldDefinition f : parsed.fields) {
            FieldDefinition existing = existingByKey.get(f.fieldKey);
            if (existing != null) {
                f.visibilityRules = existing.visibilityRules;
                f.validationRules = existing.validationRules;
            }
        }
        for (GridDefinition g : parsed.grids) {
            for (FieldDefinition c : g.columns) {
                FieldDefinition existing = existingByKey.get(c.fieldKey);
                if (existing != null) {
                    c.visibilityRules = existing.visibilityRules;
                    c.validationRules = existing.validationRules;
                }
            }
        }
    }

    // ── Row → SectionTemplateJson ────────────────────────────────────────────

    private SectionTemplateJson parseTemplate(MultipartFile file) {
        List<Map<String, String>> rows = parseRows(file);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("File contains no data rows");
        }

        List<RowWithOrder> fieldRows = new ArrayList<>();
        Map<String, GridDefinition> grids = new LinkedHashMap<>();
        Map<String, List<RowWithOrder>> gridColumnRows = new LinkedHashMap<>();

        for (Map<String, String> row : rows) {
            String rowType = req(row, "row_type");
            int order = parseOrder(row.get("display_order"));
            if ("FIELD".equalsIgnoreCase(rowType)) {
                fieldRows.add(new RowWithOrder(order, toFieldDefinition(row)));
            } else if ("GRID_COLUMN".equalsIgnoreCase(rowType)) {
                String gridKey = req(row, "grid_key");
                grids.computeIfAbsent(gridKey, k -> {
                    GridDefinition g = new GridDefinition();
                    g.gridKey = gridKey;
                    g.labelKey = req(row, "grid_label_key");
                    g.gridStorageType = req(row, "grid_storage_type");
                    g.gridBackingTable = blankToNull(row.get("grid_backing_table"));
                    return g;
                });
                gridColumnRows.computeIfAbsent(gridKey, k -> new ArrayList<>())
                    .add(new RowWithOrder(order, toFieldDefinition(row)));
            } else {
                throw new IllegalArgumentException("Unknown row_type: " + rowType);
            }
        }

        SectionTemplateJson template = new SectionTemplateJson();
        fieldRows.sort(Comparator.comparingInt(r -> r.order));
        for (RowWithOrder r : fieldRows) template.fields.add(r.field);

        for (Map.Entry<String, GridDefinition> entry : grids.entrySet()) {
            GridDefinition grid = entry.getValue();
            List<RowWithOrder> cols = gridColumnRows.get(entry.getKey());
            cols.sort(Comparator.comparingInt(r -> r.order));
            for (RowWithOrder r : cols) grid.columns.add(r.field);
            template.grids.add(grid);
        }
        return template;
    }

    private FieldDefinition toFieldDefinition(Map<String, String> row) {
        FieldDefinition f = new FieldDefinition();
        f.fieldKey = req(row, "field_key");
        f.labelKey = req(row, "label_key");
        f.fieldType = parseFieldType(req(row, "field_type"));
        f.required = "Y".equalsIgnoreCase(req(row, "required"));

        String storageType = req(row, "storage_type");
        if ("DEDICATED".equalsIgnoreCase(storageType)) {
            f.storage = StorageDefinition.dedicated(
                req(row, "dedicated_table"), req(row, "dedicated_column"));
        } else if ("EAV".equalsIgnoreCase(storageType)) {
            f.storage = StorageDefinition.eav();
        } else {
            throw new IllegalArgumentException(
                "Invalid storage_type '" + storageType + "' for field: " + row.get("field_key"));
        }

        String dropdownType = blankToNull(row.get("dropdown_source_type"));
        if (dropdownType != null) {
            switch (parseDropdownSourceType(dropdownType)) {
                case CODE_SET -> f.dropdownSource =
                    DropdownSource.codeSet(req(row, "dropdown_code_set_type"));
                case EXTERNAL -> f.dropdownSource =
                    DropdownSource.external(req(row, "dropdown_source_key"));
                case STATIC -> f.dropdownSource =
                    DropdownSource.staticSource(parseStaticOptions(req(row, "static_options")));
            }
        }
        return f;
    }

    private List<FieldOption> parseStaticOptions(String raw) {
        List<FieldOption> options = new ArrayList<>();
        for (String pair : raw.split(";")) {
            if (pair.isBlank()) continue;
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid static_options pair: " + pair);
            }
            options.add(new FieldOption(parts[0].trim(), parts[1].trim()));
        }
        return options;
    }

    private FieldType parseFieldType(String raw) {
        try {
            return FieldType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid field_type: " + raw);
        }
    }

    private DropdownSourceType parseDropdownSourceType(String raw) {
        try {
            return DropdownSourceType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid dropdown_source_type: " + raw);
        }
    }

    private int parseOrder(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required column 'display_order'");
        }
        try {
            return (int) Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid display_order: " + raw);
        }
    }

    private String req(Map<String, String> row, String key) {
        String v = row.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required column '" + key + "' in row: " + row);
        }
        return v.trim();
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private record RowWithOrder(int order, FieldDefinition field) {}

    // ── File → rows ──────────────────────────────────────────────────────────

    private List<Map<String, String>> parseRows(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            return filename.endsWith(".xlsx") ? parseXlsxRows(file) : parseCsvRows(file);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read upload file: " + e.getMessage(), e);
        }
    }

    private List<Map<String, String>> parseCsvRows(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return rows;
            String[] headers = splitCsvLine(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rows.add(toRowMap(headers, splitCsvLine(line)));
            }
        }
        return rows;
    }

    private String[] splitCsvLine(String line) {
        String[] parts = line.split(",", -1);
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        return parts;
    }

    private List<Map<String, String>> parseXlsxRows(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) return rows;

            Row headerRow = it.next();
            List<String> headerList = new ArrayList<>();
            for (Cell cell : headerRow) headerList.add(formatter.formatCellValue(cell).trim());
            String[] headers = headerList.toArray(new String[0]);

            while (it.hasNext()) {
                Row row = it.next();
                if (isBlankRow(row, formatter)) continue;
                int lastCol = Math.max(row.getLastCellNum(), headers.length);
                String[] cols = new String[lastCol];
                for (int i = 0; i < lastCol; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    cols[i] = cell != null ? formatter.formatCellValue(cell).trim() : "";
                }
                rows.add(toRowMap(headers, cols));
            }
        }
        return rows;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private Map<String, String> toRowMap(String[] headers, String[] cols) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i];
            if (h == null || h.isBlank()) continue;
            map.put(h.toLowerCase(), i < cols.length ? cols[i] : null);
        }
        return map;
    }
}
