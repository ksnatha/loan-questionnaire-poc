package com.example.loanapp.repository;

import com.example.loanapp.entity.LoanGuarantor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanGuarantorRepository extends JpaRepository<LoanGuarantor, Long> {
    List<LoanGuarantor> findByApplicationIdOrderBySortOrder(Long applicationId);
    void deleteByApplicationId(Long applicationId);
}
