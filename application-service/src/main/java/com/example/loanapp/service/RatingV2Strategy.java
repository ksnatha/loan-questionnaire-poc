package com.example.loanapp.service;

import com.example.loanapp.entity.LoanApplication;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

@Component("ratingV2")
public class RatingV2Strategy implements RiskRatingStrategy {

    private static final BigDecimal LOW_THRESHOLD  = new BigDecimal("500000");
    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("2000000");

    @Override
    public String compute(LoanApplication app, Map<String, String> answers) {
        BigDecimal amount = app.getLoanAmount();
        String base;
        if (amount == null || amount.compareTo(LOW_THRESHOLD) < 0) base = "LOW";
        else if (amount.compareTo(HIGH_THRESHOLD) <= 0) base = "MEDIUM";
        else base = "HIGH";

        // Bump risk if no collateral declared (same as v1)
        if ("NO".equalsIgnoreCase(answers.getOrDefault("requires_collateral", ""))) {
            base = switch (base) {
                case "LOW"    -> "MEDIUM";
                case "MEDIUM" -> "HIGH";
                default       -> base;
            };
        }

        // v2 enhancement: two or more pledged collateral properties reduce risk one tier
        int collateralCount = 0;
        try {
            collateralCount = Integer.parseInt(answers.getOrDefault("collateral_count", "0"));
        } catch (NumberFormatException ignored) {}
        if (collateralCount >= 2) {
            base = switch (base) {
                case "HIGH"   -> "MEDIUM";
                case "MEDIUM" -> "LOW";
                default       -> base;
            };
        }

        return base;
    }
}
