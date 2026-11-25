package com.aomanagement.service;

import com.aomanagement.dto.AuthDtos;
import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.dto.TenderDtos;
import com.aomanagement.dto.UserDtos;
import com.aomanagement.entity.*;
import com.aomanagement.repository.DocumentVersionRepository;
import com.aomanagement.repository.FavoriteTenderRepository;
import com.aomanagement.repository.TenderChecklistItemRepository;
import com.aomanagement.repository.TenderDocumentRepository;
import com.aomanagement.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class MapperService {
    private final TenderRepository tenderRepository;
    private final TenderDocumentRepository documentRepository;
    private final TenderChecklistItemRepository checklistItemRepository;
    private final FavoriteTenderRepository favoriteTenderRepository;
    private final DocumentVersionRepository documentVersionRepository;

    public AuthDtos.UserResponse toUserResponse(User user) {
        return new AuthDtos.UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), user.isEnabled(), user.isAccountLocked(), user.getLastLoginAt(), user.getLastLoginIp(), user.getLastLogoutAt());
    }

    public UserDtos.EmployeeResponse toEmployeeResponse(User user) {
        return new UserDtos.EmployeeResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.isAccountLocked(),
                user.getLastLoginAt(),
                user.getLastLoginIp(),
                user.getLastLogoutAt(),
                tenderRepository.countByAssignedEmployeeId(user.getId())
        );
    }

    public TenderDtos.TenderResponse toTenderResponse(Tender tender, boolean includeDocuments) {
        return toTenderResponse(tender, includeDocuments, null);
    }

    public TenderDtos.TenderResponse toTenderResponse(Tender tender, boolean includeDocuments, Long currentUserId) {
        List<TenderDtos.DocumentResponse> documents = includeDocuments
                ? documentRepository.findByTenderIdOrderByUploadedAtDesc(tender.getId()).stream().map(this::toDocumentResponse).toList()
                : List.of();
        long checklistTotal = checklistItemRepository.countByTenderId(tender.getId());
        long checklistCompleted = checklistItemRepository.countByTenderIdAndCompletedTrue(tender.getId());
        long missingRequiredItems = checklistItemRepository.countByTenderIdAndRequiredTrueAndCompletedFalse(tender.getId());
        int progressPercentage = checklistTotal == 0 ? 0 : (int) Math.round((checklistCompleted * 100.0) / checklistTotal);
        return new TenderDtos.TenderResponse(
                tender.getId(),
                tender.getReference(),
                tender.getMarketNumber(),
                tender.getTitle(),
                tender.getDescription(),
                tender.getOrganization(),
                tender.getPublicationDate(),
                tender.getDeadline(),
                tender.getEstimatedAmount(),
                tender.getProvisionalGuarantee(),
                tender.getFinalAmount(),
                tender.getLossReason(),
                tender.getResultDate(),
                tender.getPriority(),
                tender.getStatus(),
                tender.getResult(),
                toSimpleUser(tender.getAssignedEmployee()),
                toSimpleUser(tender.getCreatedBy()),
                tender.getCreatedAt(),
                tender.getUpdatedAt(),
                documents,
                daysRemaining(tender),
                deadlineLevel(tender),
                checklistTotal,
                checklistCompleted,
                progressPercentage,
                missingRequiredItems,
                checklistTotal > 0 && missingRequiredItems == 0,
                currentUserId != null && favoriteTenderRepository.existsByUserIdAndTenderId(currentUserId, tender.getId())
        );
    }

    public TenderDtos.DocumentResponse toDocumentResponse(TenderDocument document) {
        DocumentVersion latest = documentVersionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId()).orElse(null);
        return new TenderDtos.DocumentResponse(
                document.getId(),
                document.getTender().getId(),
                document.getFileName(),
                document.getOriginalFileName(),
                document.getDocumentType(),
                document.getFileSize(),
                toSimpleUser(document.getUploadedBy()),
                document.getUploadedAt(),
                latest == null ? 1 : latest.getVersionNumber(),
                latest == null ? document.getUploadedAt() : latest.getUploadedAt()
        );
    }

    public SprintFourDtos.TaskResponse toTaskResponse(TenderTask task) {
        Long days = task.getDeadline() == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());
        return new SprintFourDtos.TaskResponse(
                task.getId(),
                toTenderResponse(task.getTender(), false),
                task.getTitle(),
                task.getDescription(),
                toSimpleUser(task.getAssignedEmployee()),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                days,
                task.getDeadline() != null && task.getDeadline().isBefore(LocalDate.now()) && task.getStatus() != TaskStatus.DONE,
                toSimpleUser(task.getCreatedBy()),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getCompletedAt()
        );
    }

    public SprintFourDtos.ChecklistTemplateItemResponse toChecklistTemplateItemResponse(ChecklistTemplateItem item) {
        return new SprintFourDtos.ChecklistTemplateItemResponse(item.getId(), item.getLabel(), item.getPosition(), item.isRequired(), item.getExpectedDocumentType());
    }

    public SprintFourDtos.ChecklistItemResponse toChecklistItemResponse(TenderChecklistItem item) {
        boolean detected = item.getExpectedDocumentType() != null && documentRepository.findByTenderIdOrderByUploadedAtDesc(item.getTender().getId()).stream()
                .anyMatch(document -> document.getDocumentType() == item.getExpectedDocumentType());
        return new SprintFourDtos.ChecklistItemResponse(
                item.getId(),
                item.getTender().getId(),
                item.getLabel(),
                item.isRequired(),
                item.isCompleted(),
                toSimpleUser(item.getCompletedBy()),
                item.getCompletedAt(),
                item.getPosition(),
                item.getExpectedDocumentType(),
                detected,
                item.getCreatedAt()
        );
    }

    public SprintFourDtos.DocumentVersionResponse toDocumentVersionResponse(DocumentVersion version) {
        return new SprintFourDtos.DocumentVersionResponse(
                version.getId(),
                version.getDocument().getId(),
                version.getVersionNumber(),
                version.getOriginalFileName(),
                version.getFileSize(),
                toSimpleUser(version.getUploadedBy()),
                version.getUploadedAt(),
                version.getComment()
        );
    }

    public SprintFourDtos.CommentResponse toCommentResponse(TenderComment comment) {
        return new SprintFourDtos.CommentResponse(
                comment.getId(),
                comment.getTender().getId(),
                toSimpleUser(comment.getAuthor()),
                comment.isDeleted() ? "Commentaire supprime" : comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.isDeleted()
        );
    }

    public SprintFourDtos.ActivityResponse toActivityResponse(ActivityLog activity) {
        Tender tender = activity.getTender();
        return new SprintFourDtos.ActivityResponse(
                activity.getId(),
                toSimpleUser(activity.getUser()),
                tender == null ? null : tender.getId(),
                tender == null ? null : tender.getReference(),
                activity.getEntityType(),
                activity.getEntityId(),
                activity.getAction(),
                activity.getDescription(),
                activity.getCreatedAt()
        );
    }

    public TenderDtos.NotificationResponse toNotificationResponse(Notification notification) {
        Tender tender = notification.getTender();
        return new TenderDtos.NotificationResponse(
                notification.getId(),
                tender == null ? null : tender.getId(),
                tender == null ? null : tender.getReference(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    public TenderDtos.HistoryResponse toHistoryResponse(TenderHistory history) {
        return new TenderDtos.HistoryResponse(
                history.getId(),
                history.getTender() == null ? null : history.getTender().getId(),
                toSimpleUser(history.getPerformedBy()),
                history.getAction(),
                history.getFieldName(),
                history.getOldValue(),
                history.getNewValue(),
                history.getDescription(),
                history.getCreatedAt()
        );
    }

    public TenderDtos.EmailReminderResponse toEmailReminderResponse(EmailReminder reminder) {
        return new TenderDtos.EmailReminderResponse(
                reminder.getId(),
                reminder.getTender().getId(),
                reminder.getTender().getReference(),
                toSimpleUser(reminder.getEmployee()),
                reminder.getReminderType(),
                reminder.getScheduledDate(),
                reminder.getSentAt(),
                reminder.getStatus(),
                reminder.getErrorMessage(),
                reminder.getCreatedAt()
        );
    }

    public TenderDtos.SimpleUser toSimpleUser(User user) {
        if (user == null) {
            return null;
        }
        return new TenderDtos.SimpleUser(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }

    private Long daysRemaining(Tender tender) {
        if (tender.getDeadline() == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), tender.getDeadline());
    }

    private DeadlineLevel deadlineLevel(Tender tender) {
        Long days = daysRemaining(tender);
        if (days == null) {
            return DeadlineLevel.NORMAL;
        }
        if (days < 0 && tender.getStatus() != TenderStatus.CLOSED) {
            return DeadlineLevel.OVERDUE;
        }
        if (days == 0) {
            return DeadlineLevel.TODAY;
        }
        if (days <= 2) {
            return DeadlineLevel.URGENT;
        }
        if (days <= 7) {
            return DeadlineLevel.WARNING;
        }
        return DeadlineLevel.NORMAL;
    }
}
