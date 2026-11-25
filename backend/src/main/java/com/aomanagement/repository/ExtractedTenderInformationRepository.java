package com.aomanagement.repository;

import com.aomanagement.entity.ExtractedTenderInformation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractedTenderInformationRepository extends JpaRepository<ExtractedTenderInformation, Long> {
    List<ExtractedTenderInformation> findByAnalysisIdOrderByIdAsc(Long analysisId);
}
