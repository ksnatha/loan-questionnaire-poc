package com.example.questionnaire.repository;

import com.example.questionnaire.entity.TabTemplateSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TabTemplateSectionRepository extends JpaRepository<TabTemplateSection, Long> {
    List<TabTemplateSection> findByTabTemplateIdOrderByDisplayOrder(Long tabTemplateId);
    void deleteByTabTemplateId(Long tabTemplateId);
}
