package com.example.loanapp.service;

import com.example.common.template.*;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class RuleEvaluator {

    public boolean isVisible(FieldDefinition field, Map<String, String> answers) {
        if (field.visibilityRules == null || field.visibilityRules.isEmpty()) return true;
        boolean hasShowRules = field.visibilityRules.stream().anyMatch(r -> r.show);
        if (hasShowRules) {
            return field.visibilityRules.stream()
                .filter(r -> r.show)
                .anyMatch(r -> evaluateCondition(r.condition, answers));
        }
        return field.visibilityRules.stream()
            .filter(r -> !r.show)
            .noneMatch(r -> evaluateCondition(r.condition, answers));
    }

    public boolean isRequired(FieldDefinition field, Map<String, String> answers) {
        if (field.required) return true;
        if (field.validationRules == null) return false;
        return field.validationRules.stream()
            .filter(r -> r.validationType == ValidationType.REQUIRED)
            .anyMatch(r -> r.condition == null || evaluateCondition(r.condition, answers));
    }

    public boolean evaluateCondition(RuleCondition condition, Map<String, String> answers) {
        if (condition == null) return true;
        String actual = answers.getOrDefault(condition.fieldKey, "");
        return switch (condition.operator) {
            case EQUALS       -> condition.value != null && condition.value.equals(actual);
            case NOT_EQUALS   -> condition.value != null && !condition.value.equals(actual);
            case IN           -> condition.values != null && condition.values.contains(actual);
            case NOT_IN       -> condition.values == null || !condition.values.contains(actual);
            case IS_EMPTY     -> actual.isBlank();
            case IS_NOT_EMPTY -> !actual.isBlank();
        };
    }
}
