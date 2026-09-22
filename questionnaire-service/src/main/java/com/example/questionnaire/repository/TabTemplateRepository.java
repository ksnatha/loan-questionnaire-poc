package com.example.questionnaire.repository;

import com.example.questionnaire.entity.TabTemplate;
import com.example.questionnaire.entity.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TabTemplateRepository extends JpaRepository<TabTemplate, Long> {
    Optional<TabTemplate> findByTabIdAndVersion(String tabId, Integer version);
    List<TabTemplate> findByTabIdAndStatus(String tabId, TemplateStatus status);
}
