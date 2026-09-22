package com.example.loanapp.repository;

import com.example.loanapp.entity.LoanPropertyInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoanPropertyInfoRepository extends JpaRepository<LoanPropertyInfo, Long> {
    Optional<LoanPropertyInfo> findByApplicationId(Long applicationId);
}
