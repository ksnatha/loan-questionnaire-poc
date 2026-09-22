package com.example.loanapp.repository;

import com.example.loanapp.entity.LoanComplianceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoanComplianceRecordRepository extends JpaRepository<LoanComplianceRecord, Long> {
    Optional<LoanComplianceRecord> findByApplicationId(Long applicationId);
}
