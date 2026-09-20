package com.example.loanapp.repository;

import com.example.loanapp.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findAllByOrderByIdDesc();
}
