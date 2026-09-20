package com.example.loanapp.service;

import com.example.loanapp.entity.LoanApplication;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RatingV1StrategyTest {

    private final RatingV1Strategy strategy = new RatingV1Strategy();

    @Test
    void nullAmountIsLow() {
        assertEquals("LOW", strategy.compute(app(null), Map.of()));
    }

    @Test
    void belowThresholdIsLow() {
        assertEquals("LOW", strategy.compute(app("499999"), Map.of()));
    }

    @Test
    void atLowThresholdIsMedium() {
        assertEquals("MEDIUM", strategy.compute(app("500000"), Map.of()));
    }

    @Test
    void midRangeIsMedium() {
        assertEquals("MEDIUM", strategy.compute(app("1500000"), Map.of()));
    }

    @Test
    void atHighThresholdIsMedium() {
        assertEquals("MEDIUM", strategy.compute(app("2000000"), Map.of()));
    }

    @Test
    void aboveHighThresholdIsHigh() {
        assertEquals("HIGH", strategy.compute(app("2000001"), Map.of()));
    }

    // Demo 3: collateral bump
    @Test
    void lowAmountBumpsToMediumWhenNoCollateral() {
        assertEquals("MEDIUM", strategy.compute(app("100000"), Map.of("requires_collateral", "NO")));
    }

    @Test
    void mediumAmountBumpsToHighWhenNoCollateral() {
        assertEquals("HIGH", strategy.compute(app("1000000"), Map.of("requires_collateral", "NO")));
    }

    @Test
    void highAmountStaysHighEvenWithNoCollateral() {
        assertEquals("HIGH", strategy.compute(app("5000000"), Map.of("requires_collateral", "NO")));
    }

    @Test
    void yesCollateralDoesNotBump() {
        assertEquals("LOW", strategy.compute(app("100000"), Map.of("requires_collateral", "YES")));
    }

    @Test
    void missingCollateralFieldDoesNotBump() {
        assertEquals("LOW", strategy.compute(app("100000"), Map.of()));
    }

    private LoanApplication app(String amount) {
        LoanApplication a = new LoanApplication();
        if (amount != null) a.setLoanAmount(new BigDecimal(amount));
        return a;
    }
}
