package com.example.common.template;

import java.util.ArrayList;
import java.util.List;

public class GridDefinition {

    public String gridKey;
    public String labelKey;
    // Columns reuse FieldDefinition. Storage is always EAV for grid columns.
    public List<FieldDefinition> columns = new ArrayList<>();

    public GridDefinition() {}
}
