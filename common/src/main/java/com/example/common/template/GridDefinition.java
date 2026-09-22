package com.example.common.template;

import java.util.ArrayList;
import java.util.List;

public class GridDefinition {

    public String gridKey;
    public String labelKey;
    /** "EAV" (default) or "DEDICATED". EAV stores rows in USER_ANSWERS with row_index;
     *  DEDICATED stores each row as one entity in gridBackingTable. */
    public String gridStorageType = "EAV";
    /** Only set when gridStorageType = "DEDICATED". E.g. "loan_guarantor". */
    public String gridBackingTable;
    public List<FieldDefinition> columns = new ArrayList<>();

    public GridDefinition() {}
}
