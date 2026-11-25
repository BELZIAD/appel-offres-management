package com.aomanagement.repository;

import com.aomanagement.entity.ExtractedRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractedRequirementRepository extends JpaRepository<ExtractedRequirement, Long> {
    List<ExtractedRequirement> findByAnalysisIdOrderByIdAsc(Long analysisId);
}
