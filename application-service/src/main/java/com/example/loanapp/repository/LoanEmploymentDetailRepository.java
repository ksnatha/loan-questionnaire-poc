package com.example.loanapp.repository;

import com.example.loanapp.entity.LoanEmploymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoanEmploymentDetailRepository extends JpaRepository<LoanEmploymentDetail, Long> {
    Optional<LoanEmploymentDetail> findByApplicationId(Long applicationId);
}
