package com.example.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DropdownSource {

    public DropdownSourceType type;
    public String codeSetType;       // CODE_SET only
    public String sourceKey;         // EXTERNAL only
    public List<FieldOption> options; // STATIC only

    public DropdownSource() {}

    public static DropdownSource codeSet(String codeSetType) {
        DropdownSource s = new DropdownSource();
        s.type = DropdownSourceType.CODE_SET;
        s.codeSetType = codeSetType;
        return s;
    }

    public static DropdownSource external(String sourceKey) {
        DropdownSource s = new DropdownSource();
        s.type = DropdownSourceType.EXTERNAL;
        s.sourceKey = sourceKey;
        return s;
    }

    public static DropdownSource staticSource(List<FieldOption> options) {
        DropdownSource s = new DropdownSource();
        s.type = DropdownSourceType.STATIC;
        s.options = options;
        return s;
    }
}
