package com.example.lookup.service;

import com.example.lookup.dto.BulkUploadResponse;
import com.example.lookup.entity.CodeSet;
import com.example.lookup.repository.CodeSetRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class CodeSetBulkUploadService {

    private static final List<String> EXPECTED_HEADERS = List.of("type", "code", "value", "display_order");

    private final CodeSetRepository repo;

    public CodeSetBulkUploadService(CodeSetRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public BulkUploadResponse bulkUpload(MultipartFile file) {
        List<ParsedRow> rows = parse(file);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("File contains no data rows");
        }

        // Group by type, preserving first-seen order.
        Map<String, List<ParsedRow>> byType = new LinkedHashMap<>();
        for (ParsedRow row : rows) {
            byType.computeIfAbsent(row.type, k -> new ArrayList<>()).add(row);
        }

        LocalDate today = LocalDate.now();
        List<String> typesReplaced = new ArrayList<>();
        int rowsInserted = 0;

        for (Map.Entry<String, List<ParsedRow>> entry : byType.entrySet()) {
            String type = entry.getKey();
            repo.deleteByCodeSetType(type);
            int fileOrder = 1;
            for (ParsedRow row : entry.getValue()) {
                int displayOrder = row.displayOrder != null ? row.displayOrder : fileOrder;
                repo.save(new CodeSet(type, row.code, row.value, today, displayOrder));
                rowsInserted++;
                fileOrder++;
            }
            typesReplaced.add(type);
        }

        return new BulkUploadResponse(typesReplaced, rowsInserted);
    }

    // ── Parsing ──────────────────────────────────────────────────────────────

    private List<ParsedRow> parse(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        try {
            if (filename.endsWith(".xlsx")) {
                return parseXlsx(file);
            }
            return parseCsv(file);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read upload file: " + e.getMessage(), e);
        }
    }

    private List<ParsedRow> parseCsv(MultipartFile file) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return rows;
            Map<String, Integer> colIndex = indexHeaders(splitCsvLine(headerLine));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = splitCsvLine(line);
                rows.add(toRow(cols, colIndex));
            }
        }
        return rows;
    }

    private String[] splitCsvLine(String line) {
        String[] parts = line.split(",", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private List<ParsedRow> parseXlsx(MultipartFile file) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) return rows;

            Row headerRow = it.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(formatter.formatCellValue(cell).trim());
            }
            Map<String, Integer> colIndex = indexHeaders(headers.toArray(new String[0]));

            while (it.hasNext()) {
                Row row = it.next();
                if (isBlankRow(row, formatter)) continue;
                int lastCol = Math.max(row.getLastCellNum(), headers.size());
                String[] cols = new String[lastCol];
                for (int i = 0; i < lastCol; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    cols[i] = cell != null ? formatter.formatCellValue(cell).trim() : "";
                }
                rows.add(toRow(cols, colIndex));
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

    private Map<String, Integer> indexHeaders(String[] headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i] == null ? "" : headers[i].trim().toLowerCase();
            if (!h.isEmpty()) index.put(h, i);
        }
        for (String required : List.of("type", "code", "value")) {
            if (!index.containsKey(required)) {
                throw new IllegalArgumentException("Missing required column: " + required);
            }
        }
        return index;
    }

    private ParsedRow toRow(String[] cols, Map<String, Integer> colIndex) {
        ParsedRow row = new ParsedRow();
        row.type = cell(cols, colIndex, "type");
        row.code = cell(cols, colIndex, "code");
        row.value = cell(cols, colIndex, "value");
        if (row.type == null || row.type.isBlank() || row.code == null || row.code.isBlank()) {
            throw new IllegalArgumentException("Row missing required type/code: " + Arrays.toString(cols));
        }
        String displayOrderRaw = cell(cols, colIndex, "display_order");
        if (displayOrderRaw != null && !displayOrderRaw.isBlank()) {
            try {
                row.displayOrder = (int) Double.parseDouble(displayOrderRaw);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid display_order value: " + displayOrderRaw);
            }
        }
        return row;
    }

    private String cell(String[] cols, Map<String, Integer> colIndex, String name) {
        Integer i = colIndex.get(name);
        if (i == null || i >= cols.length) return null;
        String v = cols[i];
        return v == null ? null : v.trim();
    }

    private static class ParsedRow {
        String type;
        String code;
        String value;
        Integer displayOrder;
    }
}
