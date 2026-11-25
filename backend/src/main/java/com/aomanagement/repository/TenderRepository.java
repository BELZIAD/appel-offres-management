package com.aomanagement.repository;

import com.aomanagement.entity.Tender;
import com.aomanagement.entity.TenderResult;
import com.aomanagement.entity.TenderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface TenderRepository extends JpaRepository<Tender, Long>, JpaSpecificationExecutor<Tender> {
    boolean existsByReferenceIgnoreCase(String reference);
    boolean existsByReferenceIgnoreCaseAndIdNot(String reference, Long id);
    List<Tender> findByAssignedEmployeeIdOrderByDeadlineAsc(Long employeeId);
    long countByStatus(TenderStatus status);
    long countByResult(TenderResult result);
    long countByDeadlineBetween(LocalDate start, LocalDate end);
    long countByDeadlineBeforeAndStatusNot(LocalDate date, TenderStatus status);
    long countByDeadlineBetweenAndStatusNot(LocalDate start, LocalDate end, TenderStatus status);
    long countByAssignedEmployeeId(Long employeeId);
    long countByAssignedEmployeeIdAndStatus(Long employeeId, TenderStatus status);
    long countByAssignedEmployeeIdAndPriorityAndStatusNot(Long employeeId, com.aomanagement.entity.Priority priority, TenderStatus status);
    long countByAssignedEmployeeIdAndResult(Long employeeId, TenderResult result);

    List<Tender> findTop8ByOrderByCreatedAtDesc();
    List<Tender> findTop5ByStatusNotAndDeadlineGreaterThanEqualOrderByDeadlineAsc(TenderStatus status, LocalDate date);
    List<Tender> findTop5ByAssignedEmployeeIdAndStatusNotAndDeadlineGreaterThanEqualOrderByDeadlineAsc(Long employeeId, TenderStatus status, LocalDate date);
    List<Tender> findByDeadlineAndAssignedEmployeeIsNotNullAndStatusNot(LocalDate deadline, TenderStatus status);
    List<Tender> findByDeadlineAndStatusNot(LocalDate deadline, TenderStatus status);
}
