package com.example.loanapp.dto;

import java.util.List;

public class SnapshotResponse {
    public Long id;
    public String snapshotCode;
    public String status;
    public String effectiveStart;
    public List<SnapshotItemResponse> items;
}
