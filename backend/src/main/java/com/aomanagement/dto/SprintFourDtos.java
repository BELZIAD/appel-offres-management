package com.aomanagement.dto;

import com.aomanagement.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SprintFourDtos {
    public record TaskRequest(
            @NotNull Long tenderId,
            @NotBlank @Size(max = 255) String title,
            @Size(max = 5000) String description,
            @NotNull Long assignedEmployeeId,
            TaskStatus status,
            TaskPriority priority,
            @NotNull LocalDate deadline
    ) {}

    public record TaskStatusRequest(@NotNull TaskStatus status) {}

    public record TaskResponse(
            Long id,
            TenderDtos.TenderResponse tender,
            String title,
            String description,
            TenderDtos.SimpleUser assignedEmployee,
            TaskStatus status,
            TaskPriority priority,
            LocalDate deadline,
            Long daysRemaining,
            boolean overdue,
            TenderDtos.SimpleUser createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime completedAt
    ) {}

    public record ChecklistTemplateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 5000) String description,
            boolean active,
            @Valid List<ChecklistTemplateItemRequest> items
    ) {}

    public record ChecklistTemplateItemRequest(
            @NotBlank @Size(max = 255) String label,
            int position,
            boolean required,
            DocumentType expectedDocumentType
    ) {}

    public record ChecklistTemplateResponse(
            Long id,
            String name,
            String description,
            boolean active,
            LocalDateTime createdAt,
            List<ChecklistTemplateItemResponse> items
    ) {}

    public record ChecklistTemplateItemResponse(Long id, String label, int position, boolean required, DocumentType expectedDocumentType) {}

    public record ChecklistItemResponse(
            Long id,
            Long tenderId,
            String label,
            boolean required,
            boolean completed,
            TenderDtos.SimpleUser completedBy,
            LocalDateTime completedAt,
            int position,
            DocumentType expectedDocumentType,
            boolean documentDetected,
            LocalDateTime createdAt
    ) {}

    public record ChecklistStatusRequest(boolean completed) {}

    public record DocumentVersionResponse(
            Long id,
            Long documentId,
            int versionNumber,
            String originalFileName,
            long fileSize,
            TenderDtos.SimpleUser uploadedBy,
            LocalDateTime uploadedAt,
            String comment
    ) {}

    public record CommentRequest(@NotBlank @Size(max = 5000) String content) {}

    public record CommentResponse(
            Long id,
            Long tenderId,
            TenderDtos.SimpleUser author,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean deleted
    ) {}

    public record ActivityResponse(
            Long id,
            TenderDtos.SimpleUser user,
            Long tenderId,
            String tenderReference,
            String entityType,
            Long entityId,
            String action,
            String description,
            LocalDateTime createdAt
    ) {}

    public record SearchResponse(
            List<TenderDtos.TenderResponse> tenders,
            List<TaskResponse> tasks,
            List<TenderDtos.DocumentResponse> documents,
            List<TenderDtos.SimpleUser> users
    ) {}
}
