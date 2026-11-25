package com.aomanagement.repository;

import com.aomanagement.entity.TenderDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TenderDocumentRepository extends JpaRepository<TenderDocument, Long>, JpaSpecificationExecutor<TenderDocument> {
    List<TenderDocument> findByTenderIdOrderByUploadedAtDesc(Long tenderId);
}
