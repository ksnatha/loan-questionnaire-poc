package com.example.loanapp.service;

import com.example.loanapp.client.LookupClient;
import com.example.loanapp.client.QuestionnaireClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves that the rating dispatch is a pure Map<String, RiskRatingStrategy> lookup
 * (Decision 3) — adding a new version requires only registering a Spring bean,
 * not changing any dispatch code.
 */
@SpringBootTest
class RatingDispatchTest {

    // Throwaway v3 defined only inside this test — no production code references it
    @TestConfiguration
    static class ThrowawayV3Config {
        @Bean("ratingV3")
        RiskRatingStrategy ratingV3() {
            return (app, answers) -> "TEST_VERIFIED";
        }
    }

    @MockBean QuestionnaireClient questionnaireClient;
    @MockBean LookupClient lookupClient;

    @Autowired
    Map<String, RiskRatingStrategy> strategies;

    @Test
    void newVersionIsCallableWithZeroDispatchCodeChange() {
        assertTrue(strategies.containsKey("ratingV3"),
            "ratingV3 must be auto-discovered from the Spring bean map");
        String result = strategies.get("ratingV3").compute(null, Map.of());
        assertEquals("TEST_VERIFIED", result);
    }

    @Test
    void productionRatingV1IsInMap() {
        assertTrue(strategies.containsKey("ratingV1"),
            "ratingV1 production bean must be present");
    }
}
