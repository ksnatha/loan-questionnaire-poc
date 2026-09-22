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

        // FISCAL_YEAR_END
        repo.save(new CodeSet("FISCAL_YEAR_END", "JAN", "January",   epoch, 1));
        repo.save(new CodeSet("FISCAL_YEAR_END", "FEB", "February",  epoch, 2));
        repo.save(new CodeSet("FISCAL_YEAR_END", "MAR", "March",     epoch, 3));
        repo.save(new CodeSet("FISCAL_YEAR_END", "APR", "April",     epoch, 4));
        repo.save(new CodeSet("FISCAL_YEAR_END", "MAY", "May",       epoch, 5));
        repo.save(new CodeSet("FISCAL_YEAR_END", "JUN", "June",      epoch, 6));
        repo.save(new CodeSet("FISCAL_YEAR_END", "JUL", "July",      epoch, 7));
        repo.save(new CodeSet("FISCAL_YEAR_END", "AUG", "August",    epoch, 8));
        repo.save(new CodeSet("FISCAL_YEAR_END", "SEP", "September", epoch, 9));
        repo.save(new CodeSet("FISCAL_YEAR_END", "OCT", "October",   epoch, 10));
        repo.save(new CodeSet("FISCAL_YEAR_END", "NOV", "November",  epoch, 11));
        repo.save(new CodeSet("FISCAL_YEAR_END", "DEC", "December",  epoch, 12));

        // BUSINESS_STRUCTURE
        repo.save(new CodeSet("BUSINESS_STRUCTURE", "SOLE_TRADER",  "Sole Trader",  epoch, 1));
        repo.save(new CodeSet("BUSINESS_STRUCTURE", "PARTNERSHIP",  "Partnership",  epoch, 2));
        repo.save(new CodeSet("BUSINESS_STRUCTURE", "COMPANY",      "Company",      epoch, 3));
        repo.save(new CodeSet("BUSINESS_STRUCTURE", "TRUST",        "Trust",        epoch, 4));

        // SOURCE_OF_FUNDS
        repo.save(new CodeSet("SOURCE_OF_FUNDS", "BUSINESS_REVENUE",    "Business Revenue",    epoch, 1));
        repo.save(new CodeSet("SOURCE_OF_FUNDS", "INVESTMENT_RETURNS",  "Investment Returns",  epoch, 2));
        repo.save(new CodeSet("SOURCE_OF_FUNDS", "INHERITANCE",         "Inheritance",         epoch, 3));
        repo.save(new CodeSet("SOURCE_OF_FUNDS", "SAVINGS",             "Savings",             epoch, 4));
        repo.save(new CodeSet("SOURCE_OF_FUNDS", "OTHER",               "Other",               epoch, 5));

        // COUNTRY (simplified set for POC)
        repo.save(new CodeSet("COUNTRY", "AU",  "Australia",     epoch, 1));
        repo.save(new CodeSet("COUNTRY", "NZ",  "New Zealand",   epoch, 2));
        repo.save(new CodeSet("COUNTRY", "GB",  "United Kingdom",epoch, 3));
        repo.save(new CodeSet("COUNTRY", "US",  "United States", epoch, 4));
        repo.save(new CodeSet("COUNTRY", "OTH", "Other",         epoch, 5));

        // BUSINESS_TYPE
        repo.save(new CodeSet("BUSINESS_TYPE", "RETAIL",         "Retail",                epoch, 1));
        repo.save(new CodeSet("BUSINESS_TYPE", "MANUFACTURING",  "Manufacturing",         epoch, 2));
        repo.save(new CodeSet("BUSINESS_TYPE", "PROFESSIONAL",   "Professional Services", epoch, 3));
        repo.save(new CodeSet("BUSINESS_TYPE", "CONSTRUCTION",   "Construction",          epoch, 4));
        repo.save(new CodeSet("BUSINESS_TYPE", "HOSPITALITY",    "Hospitality",           epoch, 5));
        repo.save(new CodeSet("BUSINESS_TYPE", "OTHER",          "Other",                 epoch, 6));

        // INCOME_SOURCE
        repo.save(new CodeSet("INCOME_SOURCE", "SALARY",     "Salary",     epoch, 1));
        repo.save(new CodeSet("INCOME_SOURCE", "BUSINESS",   "Business",   epoch, 2));
        repo.save(new CodeSet("INCOME_SOURCE", "INVESTMENT", "Investment", epoch, 3));
        repo.save(new CodeSet("INCOME_SOURCE", "PENSION",    "Pension",    epoch, 4));
        repo.save(new CodeSet("INCOME_SOURCE", "RENTAL",     "Rental",     epoch, 5));
        repo.save(new CodeSet("INCOME_SOURCE", "OTHER",      "Other",      epoch, 6));

        // PROPERTY_USAGE
        repo.save(new CodeSet("PROPERTY_USAGE", "OWNER_OCCUPIED", "Owner Occupied", epoch, 1));
        repo.save(new CodeSet("PROPERTY_USAGE", "INVESTMENT",     "Investment",     epoch, 2));
        repo.save(new CodeSet("PROPERTY_USAGE", "COMMERCIAL",     "Commercial",     epoch, 3));
        repo.save(new CodeSet("PROPERTY_USAGE", "VACANT_LAND",    "Vacant Land",    epoch, 4));

        // ZONING_TYPE
        repo.save(new CodeSet("ZONING_TYPE", "RESIDENTIAL", "Residential", epoch, 1));
        repo.save(new CodeSet("ZONING_TYPE", "COMMERCIAL",  "Commercial",  epoch, 2));
        repo.save(new CodeSet("ZONING_TYPE", "INDUSTRIAL",  "Industrial",  epoch, 3));
        repo.save(new CodeSet("ZONING_TYPE", "RURAL",       "Rural",       epoch, 4));
        repo.save(new CodeSet("ZONING_TYPE", "MIXED_USE",   "Mixed Use",   epoch, 5));

        // GUARANTOR_RELATIONSHIP
        repo.save(new CodeSet("GUARANTOR_RELATIONSHIP", "SPOUSE",           "Spouse",           epoch, 1));
        repo.save(new CodeSet("GUARANTOR_RELATIONSHIP", "BUSINESS_PARTNER", "Business Partner", epoch, 2));
        repo.save(new CodeSet("GUARANTOR_RELATIONSHIP", "DIRECTOR",         "Director",         epoch, 3));
        repo.save(new CodeSet("GUARANTOR_RELATIONSHIP", "PARENT",           "Parent",           epoch, 4));
        repo.save(new CodeSet("GUARANTOR_RELATIONSHIP", "OTHER",            "Other",            epoch, 5));
    }
}
