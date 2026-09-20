package com.example.loanapp.dto;

public class ValidationError {
    public String fieldKey;
    public String ruleId;
    public String validationType;
    public String message;

    public ValidationError(String fieldKey, String ruleId, String validationType, String message) {
        this.fieldKey = fieldKey;
        this.ruleId = ruleId;
        this.validationType = validationType;
        this.message = message;
    }
}
