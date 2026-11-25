package com.aomanagement.repository;

import com.aomanagement.entity.DocumentSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentSummaryRepository extends JpaRepository<DocumentSummary, Long> {
    Optional<DocumentSummary> findByAnalysisId(Long analysisId);
}
