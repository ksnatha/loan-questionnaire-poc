package com.example.lookup.dto;

import java.util.List;

public class BulkUploadResponse {

    public List<String> typesReplaced;
    public int rowsInserted;

    public BulkUploadResponse() {}

    public BulkUploadResponse(List<String> typesReplaced, int rowsInserted) {
        this.typesReplaced = typesReplaced;
        this.rowsInserted = rowsInserted;
    }
}
