package com.aomanagement.repository;

import com.aomanagement.entity.ExtractedImportantDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractedImportantDateRepository extends JpaRepository<ExtractedImportantDate, Long> {
    List<ExtractedImportantDate> findByAnalysisIdOrderByDetectedDateAscIdAsc(Long analysisId);
}
