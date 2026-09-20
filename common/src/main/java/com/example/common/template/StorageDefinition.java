package com.example.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageDefinition {

    public StorageType type;
    public String tableName;   // DEDICATED only
    public String columnName;  // DEDICATED only

    public StorageDefinition() {}

    public static StorageDefinition eav() {
        StorageDefinition d = new StorageDefinition();
        d.type = StorageType.EAV;
        return d;
    }

    public static StorageDefinition dedicated(String tableName, String columnName) {
        StorageDefinition d = new StorageDefinition();
        d.type = StorageType.DEDICATED;
        d.tableName = tableName;
        d.columnName = columnName;
        return d;
    }
}
