package com.example.loanapp.repository;

import com.example.loanapp.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    List<UserAnswer> findByApplicationId(Long applicationId);

    Optional<UserAnswer> findByApplicationIdAndFieldKeyAndRowIndex(
        Long applicationId, String fieldKey, int rowIndex);

    List<UserAnswer> findByApplicationIdAndRowIndexGreaterThanOrderByRowIndex(
        Long applicationId, int rowIndex);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "DELETE FROM UserAnswer ua WHERE ua.applicationId = :appId AND ua.fieldKey IN :fieldKeys")
    void deleteGridRows(
        @org.springframework.data.repository.query.Param("appId") Long appId,
        @org.springframework.data.repository.query.Param("fieldKeys") java.util.List<String> fieldKeys);
}
