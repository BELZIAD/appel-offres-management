package com.aomanagement.dto;

import com.aomanagement.entity.SubmissionPlatform;
import com.aomanagement.entity.SubmissionReadiness;
import com.aomanagement.entity.SubmissionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SubmissionDtos {
    public record SubmissionResponse(Long id, Long tenderId, TenderDtos.SimpleUser submissionManager, SubmissionPlatform platform, SubmissionStatus status, LocalDate submissionDate, LocalTime submissionTime, String externalReference, String receiptFilePath, String notes, TenderDtos.SimpleUser submittedBy, SubmissionReadiness readiness, boolean pricingValidated, boolean checklistComplete, boolean submissionManagerAssigned, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record SubmissionConfirmRequest(@NotNull SubmissionPlatform platform, LocalDate submissionDate, LocalTime submissionTime, String externalReference, String notes, SubmissionStatus status) {}
    public record SubmissionStatusRequest(@NotNull SubmissionStatus status) {}
}
