package com.example.common.template;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationRule {

    public String ruleId;
    public RuleScope scope;
    public RuleCondition condition;  // null = unconditional; non-null = fires only when condition is true
    public String targetFieldKey;
    public ValidationType validationType;

    public Integer minLength;  // MIN_LENGTH
    public Integer maxLength;  // MAX_LENGTH
    public Double minValue;    // MIN_VALUE
    public Double maxValue;    // MAX_VALUE

    public ValidationRule() {}
}
