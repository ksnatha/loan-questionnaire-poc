package com.example.lookup.controller;

import com.example.lookup.repository.CodeSetRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CodeSetBulkUploadIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CodeSetRepository repo;

    @Test
    void replacesExistingTypeCreatesNewTypeLeavesOthersUntouched() throws Exception {
        int propertyTypeCountBefore = repo.findActiveByTypeAsOf("PROPERTY_TYPE", LocalDate.now()).size();

        String csv = "type,code,value,display_order\n" +
            "LOAN_PURPOSE,REFI_ONLY,Refinance Only,1\n" +
            "GUARANTOR_TIER,PRIMARY,Primary Guarantor,1\n" +
            "GUARANTOR_TIER,SECONDARY,Secondary Guarantor,2\n";
        MockMultipartFile file = new MockMultipartFile(
            "file", "codesets.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/code-sets/bulk-upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rowsInserted").value(3))
            .andExpect(jsonPath("$.typesReplaced", org.hamcrest.Matchers.containsInAnyOrder(
                "LOAN_PURPOSE", "GUARANTOR_TIER")));

        List<com.example.lookup.entity.CodeSet> loanPurpose =
            repo.findActiveByTypeAsOf("LOAN_PURPOSE", LocalDate.now());
        assertEquals(1, loanPurpose.size());
        assertEquals("REFI_ONLY", loanPurpose.get(0).getCode());

        List<com.example.lookup.entity.CodeSet> guarantorTier =
            repo.findActiveByTypeAsOf("GUARANTOR_TIER", LocalDate.now());
        assertEquals(2, guarantorTier.size());

        int propertyTypeCountAfter = repo.findActiveByTypeAsOf("PROPERTY_TYPE", LocalDate.now()).size();
        assertEquals(propertyTypeCountBefore, propertyTypeCountAfter);
    }

    @Test
    void defaultsDisplayOrderToFileRowOrderWhenOmitted() throws Exception {
        String csv = "type,code,value\n" +
            "NO_ORDER_TYPE,A,Alpha\n" +
            "NO_ORDER_TYPE,B,Beta\n";
        MockMultipartFile file = new MockMultipartFile(
            "file", "codesets.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/code-sets/bulk-upload").file(file))
            .andExpect(status().isOk());

        List<com.example.lookup.entity.CodeSet> rows =
            repo.findActiveByTypeAsOf("NO_ORDER_TYPE", LocalDate.now());
        assertEquals(2, rows.size());
        assertEquals("A", rows.get(0).getCode());
        assertEquals(1, rows.get(0).getDisplayOrder());
        assertEquals("B", rows.get(1).getCode());
        assertEquals(2, rows.get(1).getDisplayOrder());
    }

    @Test
    void parsesXlsxUpload() throws Exception {
        byte[] xlsx;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("codesets");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("type");
            header.createCell(1).setCellValue("code");
            header.createCell(2).setCellValue("value");
            header.createCell(3).setCellValue("display_order");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("XLSX_TYPE");
            row1.createCell(1).setCellValue("ONE");
            row1.createCell(2).setCellValue("One");
            row1.createCell(3).setCellValue(1);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("XLSX_TYPE");
            row2.createCell(1).setCellValue("TWO");
            row2.createCell(2).setCellValue("Two");
            row2.createCell(3).setCellValue(2);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            xlsx = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
            "file", "codesets.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);

        mockMvc.perform(multipart("/code-sets/bulk-upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rowsInserted").value(2));

        List<com.example.lookup.entity.CodeSet> rows =
            repo.findActiveByTypeAsOf("XLSX_TYPE", LocalDate.now());
        assertEquals(2, rows.size());
        assertEquals("ONE", rows.get(0).getCode());
    }

    @Test
    void rejectsMissingRequiredColumn() throws Exception {
        String csv = "type,value\nMISSING_CODE_COL,Some Value\n";
        MockMultipartFile file = new MockMultipartFile(
            "file", "bad.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/code-sets/bulk-upload").file(file))
            .andExpect(status().isBadRequest());
    }
}
