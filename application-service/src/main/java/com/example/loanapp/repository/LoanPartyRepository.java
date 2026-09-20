package com.example.loanapp.repository;

import com.example.loanapp.entity.LoanParty;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanPartyRepository extends JpaRepository<LoanParty, Long> {
    List<LoanParty> findByApplicationId(Long applicationId);
}
