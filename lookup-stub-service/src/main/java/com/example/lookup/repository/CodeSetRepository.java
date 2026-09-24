package com.example.lookup.repository;

import com.example.lookup.entity.CodeSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CodeSetRepository extends JpaRepository<CodeSet, Long> {

    @Query("SELECT c FROM CodeSet c WHERE c.codeSetType = :type AND c.activeInd = 'Y' " +
           "AND c.startDate <= :asOf AND (c.endDate IS NULL OR c.endDate >= :asOf) " +
           "ORDER BY c.displayOrder, c.code")
    List<CodeSet> findActiveByTypeAsOf(@Param("type") String type, @Param("asOf") LocalDate asOf);

    @Query("SELECT c FROM CodeSet c WHERE c.codeSetType = :type AND c.code = :code " +
           "AND c.activeInd = 'Y' AND c.startDate <= :asOf AND (c.endDate IS NULL OR c.endDate >= :asOf)")
    Optional<CodeSet> findByTypeAndCodeAsOf(@Param("type") String type,
                                             @Param("code") String code,
                                             @Param("asOf") LocalDate asOf);

    void deleteByCodeSetType(String codeSetType);
}
