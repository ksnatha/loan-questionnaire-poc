package com.example.questionnaire.dto;

public class BulkUploadFieldsResponse {

    public String sectionId;
    public Integer draftVersion;
    public int fieldCount;
    public int gridCount;

    public BulkUploadFieldsResponse() {}

    public BulkUploadFieldsResponse(String sectionId, Integer draftVersion, int fieldCount, int gridCount) {
        this.sectionId = sectionId;
        this.draftVersion = draftVersion;
        this.fieldCount = fieldCount;
        this.gridCount = gridCount;
    }
}
