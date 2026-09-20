package com.example.questionnaire.repository;

import com.example.questionnaire.entity.TabTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TabTemplateRepository extends JpaRepository<TabTemplate, Long> {
    Optional<TabTemplate> findByTabIdAndVersion(String tabId, Integer version);
}
