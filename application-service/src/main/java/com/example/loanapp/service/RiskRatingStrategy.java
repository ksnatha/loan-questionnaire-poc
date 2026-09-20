package com.example.loanapp.service;

import com.example.loanapp.entity.LoanApplication;
import java.util.Map;

public interface RiskRatingStrategy {
    String compute(LoanApplication app, Map<String, String> answers);
}
