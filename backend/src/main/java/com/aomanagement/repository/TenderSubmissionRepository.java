package com.aomanagement.repository;

import com.aomanagement.entity.SubmissionStatus;
import com.aomanagement.entity.TenderSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TenderSubmissionRepository extends JpaRepository<TenderSubmission, Long> {
    Optional<TenderSubmission> findByTenderId(Long tenderId);
    List<TenderSubmission> findBySubmissionManagerIdOrderByTenderDeadlineAsc(Long employeeId);
    long countByStatus(SubmissionStatus status);
    long countBySubmissionDateAndStatusIn(LocalDate date, List<SubmissionStatus> statuses);
}
