package com.example.lookup.repository;

import com.example.lookup.entity.CodeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CodeSetRepositoryTest {

    @Autowired CodeSetRepository repo;

    private static final LocalDate EPOCH = LocalDate.of(2000, 1, 1);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 20);
    private static final LocalDate FUTURE = LocalDate.of(2030, 1, 1);
    private static final LocalDate PAST = LocalDate.of(2024, 1, 1);

    @BeforeEach
    void setup() {
        repo.deleteAll();

        // active — no end date
        repo.save(new CodeSet("TEST", "ACTIVE_CODE", "Active", EPOCH, 1));

        // expired — end_date before TODAY
        CodeSet expired = new CodeSet("TEST", "EXPIRED_CODE", "Expired", EPOCH, 2);
        expired.setEndDate(PAST);
        repo.save(expired);

        // future — start_date after TODAY
        repo.save(new CodeSet("TEST", "FUTURE_CODE", "Future", FUTURE, 3));

        // inactive — activeInd = N
        CodeSet inactive = new CodeSet("TEST", "INACTIVE_CODE", "Inactive", EPOCH, 4);
        inactive.setActiveInd("N");
        repo.save(inactive);
    }

    @Test
    void returnsActiveCodeSet() {
        List<CodeSet> results = repo.findActiveByTypeAsOf("TEST", TODAY);
        assertEquals(1, results.size());
        assertEquals("ACTIVE_CODE", results.get(0).getCode());
    }

    @Test
    void excludesExpiredCodeSet() {
        List<CodeSet> results = repo.findActiveByTypeAsOf("TEST", TODAY);
        assertTrue(results.stream().noneMatch(cs -> "EXPIRED_CODE".equals(cs.getCode())));
    }

    @Test
    void excludesFutureDatedCodeSet() {
        List<CodeSet> results = repo.findActiveByTypeAsOf("TEST", TODAY);
        assertTrue(results.stream().noneMatch(cs -> "FUTURE_CODE".equals(cs.getCode())));
    }

    @Test
    void excludesInactiveCodeSet() {
        List<CodeSet> results = repo.findActiveByTypeAsOf("TEST", TODAY);
        assertTrue(results.stream().noneMatch(cs -> "INACTIVE_CODE".equals(cs.getCode())));
    }

    @Test
    void futureDatedCodeSetVisibleAfterStartDate() {
        List<CodeSet> results = repo.findActiveByTypeAsOf("TEST", FUTURE);
        assertTrue(results.stream().anyMatch(cs -> "FUTURE_CODE".equals(cs.getCode())));
    }
}
