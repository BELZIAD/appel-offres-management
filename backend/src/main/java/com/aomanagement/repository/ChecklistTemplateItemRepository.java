package com.aomanagement.repository;

import com.aomanagement.entity.ChecklistTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistTemplateItemRepository extends JpaRepository<ChecklistTemplateItem, Long> {
    List<ChecklistTemplateItem> findByTemplateIdOrderByPositionAsc(Long templateId);
    void deleteByTemplateId(Long templateId);
}
