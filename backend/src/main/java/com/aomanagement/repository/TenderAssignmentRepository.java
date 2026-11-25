package com.aomanagement.repository;

import com.aomanagement.entity.TenderAssignment;
import com.aomanagement.entity.TenderAssignmentRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenderAssignmentRepository extends JpaRepository<TenderAssignment, Long> {
    List<TenderAssignment> findByTenderIdAndActiveTrueOrderByAssignmentRoleAscEmployeeLastNameAsc(Long tenderId);
    List<TenderAssignment> findByEmployeeIdAndActiveTrueOrderByTenderDeadlineAsc(Long employeeId);
    List<TenderAssignment> findByEmployeeIdAndAssignmentRoleAndActiveTrueOrderByTenderDeadlineAsc(Long employeeId, TenderAssignmentRole role);
    boolean existsByTenderIdAndEmployeeIdAndAssignmentRoleAndActiveTrue(Long tenderId, Long employeeId, TenderAssignmentRole role);
    boolean existsByTenderIdAndEmployeeIdAndActiveTrue(Long tenderId, Long employeeId);
    boolean existsByTenderIdAndAssignmentRoleAndActiveTrue(Long tenderId, TenderAssignmentRole role);
    Optional<TenderAssignment> findByTenderIdAndEmployeeIdAndAssignmentRoleAndActiveTrue(Long tenderId, Long employeeId, TenderAssignmentRole role);
}
