package com.aomanagement.repository;

import com.aomanagement.entity.ChecklistTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {
    List<ChecklistTemplate> findAllByOrderByCreatedAtDesc();
}
