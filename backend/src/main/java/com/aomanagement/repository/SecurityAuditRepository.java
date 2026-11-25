package com.aomanagement.repository;

import com.aomanagement.entity.SecurityAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, Long> {
    Page<SecurityAudit> findByEmailContainingIgnoreCaseOrderByCreatedAtDesc(String email, Pageable pageable);
    Page<SecurityAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
