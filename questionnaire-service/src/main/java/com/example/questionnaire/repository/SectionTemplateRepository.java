package com.example.questionnaire.repository;

import com.example.questionnaire.entity.SectionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SectionTemplateRepository extends JpaRepository<SectionTemplate, Long> {
    Optional<SectionTemplate> findBySectionIdAndVersion(String sectionId, Integer version);
    List<SectionTemplate> findBySectionIdOrderByVersionDesc(String sectionId);
}
