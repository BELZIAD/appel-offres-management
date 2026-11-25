package com.aomanagement.dto;

import com.aomanagement.entity.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TenderDtos {
    public record TenderRequest(
            @NotBlank String reference,
            String marketNumber,
            @NotBlank String title,
            String description,
            @NotBlank String organization,
            LocalDate publicationDate,
            @NotNull LocalDate deadline,
            BigDecimal estimatedAmount,
            BigDecimal provisionalGuarantee,
            BigDecimal finalAmount,
            String lossReason,
            Priority priority,
            TenderStatus status,
            TenderResult result,
            Long assignedEmployeeId
    ) {
        @AssertTrue(message = "La date limite doit être supérieure ou égale à la date de publication")
        public boolean isDeadlineValid() {
            return publicationDate == null || deadline == null || !deadline.isBefore(publicationDate);
        }
    }

    public record TenderResponse(
            Long id,
            String reference,
            String marketNumber,
            String title,
            String description,
            String organization,
            LocalDate publicationDate,
            LocalDate deadline,
            BigDecimal estimatedAmount,
            BigDecimal provisionalGuarantee,
            BigDecimal finalAmount,
            String lossReason,
            LocalDate resultDate,
            Priority priority,
            TenderStatus status,
            TenderResult result,
            SimpleUser assignedEmployee,
            SimpleUser createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<DocumentResponse> documents
            ,
            Long daysRemaining,
            DeadlineLevel deadlineLevel,
            long checklistTotal,
            long checklistCompleted,
            int progressPercentage,
            long missingRequiredItems,
            boolean checklistComplete,
            boolean favorite
    ) {}

    public record SimpleUser(Long id, String firstName, String lastName, String email) {}

    public record DocumentResponse(
            Long id,
            Long tenderId,
            String fileName,
            String originalFileName,
            DocumentType documentType,
            long fileSize,
            SimpleUser uploadedBy,
            LocalDateTime uploadedAt,
            int currentVersion,
            LocalDateTime latestVersionAt
    ) {}

    public record DashboardStats(
            long total,
            long newCount,
            long inProgress,
            long submitted,
            long won,
            long lost,
            long readyToSubmit,
            long urgent,
            long overdue,
            long dueInSevenDays,
            List<TenderResponse> recentTenders,
            List<TenderResponse> nextDeadlines
    ) {}

    public record EmployeeDashboardStats(long myTenders, long inProgress, long urgent, long readyToSubmit, long submitted, long won, long lost, List<TenderResponse> nextDeadlines) {}

    public record StatusUpdateRequest(@NotNull TenderStatus status) {}
    public record ResultUpdateRequest(@NotNull TenderResult result, BigDecimal finalAmount, String lossReason) {}

    public record TenderSearchCriteria(
            String search,
            TenderStatus status,
            TenderResult result,
            Priority priority,
            Long employeeId,
            String organization,
            LocalDate deadlineFrom,
            LocalDate deadlineTo
    ) {}

    public record NotificationResponse(
            Long id,
            Long tenderId,
            String tenderReference,
            NotificationType type,
            String title,
            String message,
            boolean read,
            LocalDateTime createdAt,
            LocalDateTime readAt
    ) {}

    public record UnreadCountResponse(long count) {}

    public record HistoryResponse(
            Long id,
            Long tenderId,
            SimpleUser performedBy,
            TenderHistoryAction action,
            String fieldName,
            String oldValue,
            String newValue,
            String description,
            LocalDateTime createdAt
    ) {}

    public record EmailReminderResponse(
            Long id,
            Long tenderId,
            String tenderReference,
            SimpleUser employee,
            ReminderType reminderType,
            LocalDate scheduledDate,
            LocalDateTime sentAt,
            EmailReminderStatus status,
            String errorMessage,
            LocalDateTime createdAt
    ) {}
}
