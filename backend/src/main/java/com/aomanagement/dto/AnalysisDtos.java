package com.aomanagement.dto;

import com.aomanagement.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AnalysisDtos {
    public record PdfExtractionResult(String text, int pageCount, int textLength, boolean requiresOcr, ExtractionMethod method) {}

    public record AnalysisResponse(
            Long id,
            Long tenderId,
            Long documentId,
            Long documentVersionId,
            int versionNumber,
            String documentName,
            AnalysisStatus status,
            ExtractionMethod extractionMethod,
            String detectedLanguage,
            int pageCount,
            int textLength,
            boolean requiresOcr,
            String analysisEngineVersion,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            String errorMessage,
            LocalDateTime createdAt,
            List<ExtractedFieldResponse> fields,
            List<RequirementResponse> requirements,
            List<ImportantDateResponse> importantDates,
            SummaryResponse summary
    ) {}

    public record ExtractedFieldResponse(
            Long id,
            String field,
            String value,
            BigDecimal confidence,
            Integer sourcePage,
            String sourceExcerpt,
            boolean validated,
            boolean ignored,
            boolean conflict,
            String currentValue
    ) {}

    public record RequirementResponse(
            Long id,
            RequirementCategory category,
            String label,
            String description,
            boolean required,
            Integer sourcePage,
            BigDecimal confidence,
            RequirementStatus status
    ) {}

    public record ImportantDateResponse(Long id, ImportantDateType type, LocalDate date, String label, Integer sourcePage, BigDecimal confidence) {}

    public record SummaryResponse(String shortSummary, String keyPoints, String importantDates, String requiredDocuments, String financialConditions, String technicalConditions, String attentionPoints) {}

    public record ApplyFieldRequest(@NotBlank String field, @NotBlank String value, Long extractedFieldId) {}
    public record ApplyChecklistRequest(@NotNull Long analysisId, List<Long> requirementIds) {}
    public record DocumentSearchResult(Integer page, String snippet) {}
}
