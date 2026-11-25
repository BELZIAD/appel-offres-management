package com.aomanagement.repository;

import com.aomanagement.entity.TenderChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenderChecklistItemRepository extends JpaRepository<TenderChecklistItem, Long> {
    List<TenderChecklistItem> findByTenderIdOrderByPositionAsc(Long tenderId);
    long countByTenderId(Long tenderId);
    long countByTenderIdAndCompletedTrue(Long tenderId);
    long countByTenderIdAndRequiredTrueAndCompletedFalse(Long tenderId);
}
