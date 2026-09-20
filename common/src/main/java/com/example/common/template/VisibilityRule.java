package com.example.common.template;

public class VisibilityRule {

    public String ruleId;
    public RuleScope scope;
    public RuleCondition condition;
    public String targetFieldKey;
    public boolean show; // true = show when condition met; false = hide when condition met

    public VisibilityRule() {}
}
