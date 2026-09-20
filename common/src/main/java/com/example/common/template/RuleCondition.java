package com.example.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleCondition {

    public String fieldKey;
    public RuleOperator operator;
    public String value;        // EQUALS / NOT_EQUALS
    public List<String> values; // IN / NOT_IN

    public RuleCondition() {}
}
