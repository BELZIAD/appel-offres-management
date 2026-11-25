package com.aomanagement.repository;

import com.aomanagement.entity.AnalysisStatus;
import com.aomanagement.entity.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, Long> {
    List<DocumentAnalysis> findByDocumentVersionDocumentIdOrderByCreatedAtDesc(Long documentId);
    Optional<DocumentAnalysis> findTopByDocumentVersionDocumentIdOrderByCreatedAtDesc(Long documentId);
    boolean existsByDocumentVersionIdAndStatusIn(Long documentVersionId, List<AnalysisStatus> statuses);
}
