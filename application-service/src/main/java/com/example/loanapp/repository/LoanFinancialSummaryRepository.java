package com.example.loanapp.repository;

import com.example.loanapp.entity.LoanFinancialSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoanFinancialSummaryRepository extends JpaRepository<LoanFinancialSummary, Long> {
    Optional<LoanFinancialSummary> findByApplicationId(Long applicationId);
}
