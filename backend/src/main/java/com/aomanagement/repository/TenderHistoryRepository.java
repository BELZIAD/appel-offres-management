package com.aomanagement.repository;

import com.aomanagement.entity.TenderHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TenderHistoryRepository extends JpaRepository<TenderHistory, Long>, JpaSpecificationExecutor<TenderHistory> {
    Page<TenderHistory> findByTenderIdOrderByCreatedAtDesc(Long tenderId, Pageable pageable);
    Page<TenderHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
