package com.example.lookup.init;

import com.example.lookup.entity.CodeSet;
import com.example.lookup.repository.CodeSetRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CodeSetRepository repo;

    public DataInitializer(CodeSetRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;

        LocalDate epoch = LocalDate.of(2000, 1, 1);

        // LOAN_PURPOSE
        repo.save(new CodeSet("LOAN_PURPOSE", "PURCHASE",      "Purchase",          epoch, 1));
        repo.save(new CodeSet("LOAN_PURPOSE", "REFI",          "Refinance",         epoch, 2));
        repo.save(new CodeSet("LOAN_PURPOSE", "CONSTRUCTION",  "Construction",      epoch, 3));
        repo.save(new CodeSet("LOAN_PURPOSE", "EQUITY_RELEASE","Equity Release",    epoch, 4));
        repo.save(new CodeSet("LOAN_PURPOSE", "OTHER",         "Other",             epoch, 5));

        // REPAYMENT_FREQ
        repo.save(new CodeSet("REPAYMENT_FREQ", "MONTHLY",     "Monthly",           epoch, 1));
        repo.save(new CodeSet("REPAYMENT_FREQ", "QUARTERLY",   "Quarterly",         epoch, 2));
        repo.save(new CodeSet("REPAYMENT_FREQ", "SEMI_ANNUAL", "Semi-Annual",       epoch, 3));
        repo.save(new CodeSet("REPAYMENT_FREQ", "ANNUAL",      "Annual",            epoch, 4));

        // INTEREST_RATE_TYPE
        repo.save(new CodeSet("INTEREST_RATE_TYPE", "FIXED",   "Fixed",             epoch, 1));
        repo.save(new CodeSet("INTEREST_RATE_TYPE", "VARIABLE","Variable",          epoch, 2));

        // PROPERTY_TYPE
        repo.save(new CodeSet("PROPERTY_TYPE", "RESIDENTIAL", "Residential",        epoch, 1));
        repo.save(new CodeSet("PROPERTY_TYPE", "COMMERCIAL",  "Commercial",         epoch, 2));
        repo.save(new CodeSet("PROPERTY_TYPE", "INDUSTRIAL",  "Industrial",         epoch, 3));
        repo.save(new CodeSet("PROPERTY_TYPE", "RURAL",       "Rural",              epoch, 4));

        // LIEN_POSITION
        repo.save(new CodeSet("LIEN_POSITION", "FIRST",  "First",                   epoch, 1));
        repo.save(new CodeSet("LIEN_POSITION", "SECOND", "Second",                  epoch, 2));
        repo.save(new CodeSet("LIEN_POSITION", "THIRD",  "Third",                   epoch, 3));
    }
}
