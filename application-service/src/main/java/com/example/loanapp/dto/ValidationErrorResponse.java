package com.example.loanapp.dto;

import java.util.List;

public class ValidationErrorResponse {
    public int status = 422;
    public List<ValidationError> errors;

    public ValidationErrorResponse(List<ValidationError> errors) {
        this.errors = errors;
    }
}
