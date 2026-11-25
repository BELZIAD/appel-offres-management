package com.aomanagement.repository;

import com.aomanagement.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long documentId);
    Optional<DocumentVersion> findTopByDocumentIdOrderByVersionNumberDesc(Long documentId);
    long countByDocumentId(Long documentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<DocumentVersion> findWithLockByDocumentIdOrderByVersionNumberDesc(Long documentId);
}
