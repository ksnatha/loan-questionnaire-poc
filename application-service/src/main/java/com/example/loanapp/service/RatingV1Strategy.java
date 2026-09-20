package com.example.loanapp.service;

import com.example.loanapp.entity.LoanApplication;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

@Component("ratingV1")
public class RatingV1Strategy implements RiskRatingStrategy {

    private static final BigDecimal LOW_THRESHOLD  = new BigDecimal("500000");
    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("2000000");

    @Override
    public String compute(LoanApplication app, Map<String, String> answers) {
        BigDecimal amount = app.getLoanAmount();
        String base;
        if (amount == null || amount.compareTo(LOW_THRESHOLD) < 0) {
            base = "LOW";
        } else if (amount.compareTo(HIGH_THRESHOLD) <= 0) {
            base = "MEDIUM";
        } else {
            base = "HIGH";
        }
        // Bump one tier when borrower indicated no collateral required
        if ("NO".equalsIgnoreCase(answers.getOrDefault("requires_collateral", ""))) {
            base = switch (base) {
                case "LOW"    -> "MEDIUM";
                case "MEDIUM" -> "HIGH";
                default       -> base; // HIGH stays HIGH
            };
        }
        return base;
    }
}
