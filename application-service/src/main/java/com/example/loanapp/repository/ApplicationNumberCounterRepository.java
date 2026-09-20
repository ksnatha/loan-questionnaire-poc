package com.example.loanapp.repository;

import com.example.loanapp.entity.ApplicationNumberCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ApplicationNumberCounterRepository extends JpaRepository<ApplicationNumberCounter, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ApplicationNumberCounter c WHERE c.year = :year")
    Optional<ApplicationNumberCounter> findByYearForUpdate(@Param("year") Integer year);
}
