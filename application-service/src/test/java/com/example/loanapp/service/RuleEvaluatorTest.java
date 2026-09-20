package com.example.loanapp.service;

import com.example.common.template.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluatorTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();

    // --- Demo 1: visibility ---

    @Test
    void fieldWithNoRulesIsAlwaysVisible() {
        FieldDefinition field = fieldWithVisibilityRules();
        assertTrue(evaluator.isVisible(field, Map.of()));
    }

    @Test
    void showRuleHidesFieldWhenConditionNotMet() {
        FieldDefinition field = fieldWithVisibilityRules(showWhenEquals("trigger", "YES"));
        assertFalse(evaluator.isVisible(field, Map.of("trigger", "NO")));
    }

    @Test
    void showRuleRevealFieldWhenConditionMet() {
        FieldDefinition field = fieldWithVisibilityRules(showWhenEquals("trigger", "YES"));
        assertTrue(evaluator.isVisible(field, Map.of("trigger", "YES")));
    }

    @Test
    void missingTriggerFieldValueTreatedAsEmpty() {
        FieldDefinition field = fieldWithVisibilityRules(showWhenEquals("trigger", "YES"));
        assertFalse(evaluator.isVisible(field, Map.of()));
    }

    // --- Demo 2: conditional required ---

    @Test
    void baseRequiredTrueAlwaysRequired() {
        FieldDefinition field = new FieldDefinition();
        field.required = true;
        assertTrue(evaluator.isRequired(field, Map.of()));
    }

    @Test
    void conditionalRequiredFiresWhenConditionMet() {
        FieldDefinition field = new FieldDefinition();
        field.required = false;
        field.validationRules = List.of(requiredWhenEquals("rate_type", "VARIABLE"));
        assertTrue(evaluator.isRequired(field, Map.of("rate_type", "VARIABLE")));
    }

    @Test
    void conditionalRequiredDoesNotFireWhenConditionNotMet() {
        FieldDefinition field = new FieldDefinition();
        field.required = false;
        field.validationRules = List.of(requiredWhenEquals("rate_type", "VARIABLE"));
        assertFalse(evaluator.isRequired(field, Map.of("rate_type", "FIXED")));
    }

    @Test
    void conditionalRequiredDoesNotFireWhenTriggerAbsent() {
        FieldDefinition field = new FieldDefinition();
        field.required = false;
        field.validationRules = List.of(requiredWhenEquals("rate_type", "VARIABLE"));
        assertFalse(evaluator.isRequired(field, Map.of()));
    }

    // --- Proof: ROW-scoped rule uses the same evaluator (Decision 7) ---

    @Test
    void rowScopedRuleEvaluatesWithSameEvaluatorClass() {
        // RuleScope.ROW — evaluator receives row's own answers, same code path as SECTION
        FieldDefinition lienPosition = new FieldDefinition();
        lienPosition.required = false;
        ValidationRule rule = new ValidationRule();
        rule.ruleId = "lien-required-when-commercial";
        rule.scope = RuleScope.ROW;  // ROW scope — caller is responsible for passing row answers
        rule.validationType = ValidationType.REQUIRED;
        rule.condition = eqCondition("property_type", "COMMERCIAL");
        lienPosition.validationRules = List.of(rule);

        // Pass the ROW's own answer map — same evaluator.isRequired() call as SECTION
        assertTrue(evaluator.isRequired(lienPosition, Map.of("property_type", "COMMERCIAL")));
        assertFalse(evaluator.isRequired(lienPosition, Map.of("property_type", "RESIDENTIAL")));
    }

    // --- helpers ---

    private FieldDefinition fieldWithVisibilityRules(VisibilityRule... rules) {
        FieldDefinition f = new FieldDefinition();
        f.visibilityRules = List.of(rules);
        return f;
    }

    private VisibilityRule showWhenEquals(String fieldKey, String value) {
        VisibilityRule r = new VisibilityRule();
        r.ruleId = "test-rule";
        r.scope = RuleScope.SECTION;
        r.show = true;
        r.condition = eqCondition(fieldKey, value);
        return r;
    }

    private ValidationRule requiredWhenEquals(String fieldKey, String value) {
        ValidationRule r = new ValidationRule();
        r.ruleId = "test-required";
        r.scope = RuleScope.SECTION;
        r.validationType = ValidationType.REQUIRED;
        r.condition = eqCondition(fieldKey, value);
        return r;
    }

    private RuleCondition eqCondition(String fieldKey, String value) {
        RuleCondition c = new RuleCondition();
        c.fieldKey = fieldKey;
        c.operator = RuleOperator.EQUALS;
        c.value = value;
        return c;
    }
}
