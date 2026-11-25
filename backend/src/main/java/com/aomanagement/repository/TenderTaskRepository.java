package com.aomanagement.repository;

import com.aomanagement.entity.TaskStatus;
import com.aomanagement.entity.TenderTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface TenderTaskRepository extends JpaRepository<TenderTask, Long>, JpaSpecificationExecutor<TenderTask> {
    List<TenderTask> findByAssignedEmployeeIdOrderByDeadlineAsc(Long employeeId);
    List<TenderTask> findByDeadlineAndStatusNot(LocalDate deadline, TaskStatus status);
    long countByStatusNot(TaskStatus status);
    long countByDeadlineBeforeAndStatusNot(LocalDate deadline, TaskStatus status);
    long countByStatus(TaskStatus status);
    long countByTenderAssignedEmployeeIdAndStatusNot(Long employeeId, TaskStatus status);
    long countByAssignedEmployeeIdAndStatus(Long employeeId, TaskStatus status);
    long countByAssignedEmployeeIdAndDeadlineBeforeAndStatusNot(Long employeeId, LocalDate deadline, TaskStatus status);
    long countByAssignedEmployeeIdAndDeadlineAndStatusNot(Long employeeId, LocalDate deadline, TaskStatus status);
    long countByAssignedEmployeeIdAndCompletedAtBetween(Long employeeId, java.time.LocalDateTime from, java.time.LocalDateTime to);
}
