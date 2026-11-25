package com.aomanagement.dto;

import com.aomanagement.entity.TenderAssignmentRole;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class AssignmentDtos {
    public record AssignmentRequest(@NotNull Long employeeId, @NotNull TenderAssignmentRole assignmentRole, Boolean active) {}
    public record AssignmentResponse(Long id, TenderDtos.SimpleUser employee, TenderAssignmentRole assignmentRole, TenderDtos.SimpleUser assignedBy, LocalDateTime assignedAt, boolean active) {}
    public record TeamResponse(List<AssignmentResponse> assignments) {}
}
