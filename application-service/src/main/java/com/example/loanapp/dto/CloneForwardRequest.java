package com.example.loanapp.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class CloneForwardRequest {

    public String newSnapshotCode;
    public Map<String, ItemOverride> overrides = new LinkedHashMap<>();

    public static class ItemOverride {
        public Integer versionValue;
        public String strategyBeanName;
        public String entityId;
    }
}
