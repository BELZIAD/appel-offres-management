package com.aomanagement.service;

import com.aomanagement.dto.AnalysisDtos;
import com.aomanagement.entity.*;
import com.aomanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentAnalysisMapper {
    private final ExtractedTenderInformationRepository informationRepository;
    private final ExtractedRequirementRepository requirementRepository;
    private final ExtractedImportantDateRepository dateRepository;
    private final DocumentSummaryRepository summaryRepository;
    private final MapperService mapperService;

    public AnalysisDtos.AnalysisResponse toResponse(DocumentAnalysis analysis) {
        Tender tender = analysis.getDocumentVersion().getDocument().getTender();
        return new AnalysisDtos.AnalysisResponse(
                analysis.getId(),
                tender.getId(),
                analysis.getDocumentVersion().getDocument().getId(),
                analysis.getDocumentVersion().getId(),
                analysis.getDocumentVersion().getVersionNumber(),
                analysis.getDocumentVersion().getOriginalFileName(),
                analysis.getStatus(),
                analysis.getExtractionMethod(),
                analysis.getDetectedLanguage(),
                analysis.getPageCount(),
                analysis.getTextLength(),
                analysis.isRequiresOcr(),
                analysis.getAnalysisEngineVersion(),
                analysis.getStartedAt(),
                analysis.getCompletedAt(),
                analysis.getErrorMessage(),
                analysis.getCreatedAt(),
                informationRepository.findByAnalysisIdOrderByIdAsc(analysis.getId()).stream().map(info -> toField(info, tender)).toList(),
                requirementRepository.findByAnalysisIdOrderByIdAsc(analysis.getId()).stream().map(this::toRequirement).toList(),
                dateRepository.findByAnalysisIdOrderByDetectedDateAscIdAsc(analysis.getId()).stream().map(this::toImportantDate).toList(),
                summaryRepository.findByAnalysisId(analysis.getId()).map(this::toSummary).orElse(null)
        );
    }

    private AnalysisDtos.ExtractedFieldResponse toField(ExtractedTenderInformation info, Tender tender) {
        String current = currentValue(info.getFieldName(), tender);
        boolean conflict = current != null && info.getFieldValue() != null && !current.equals(info.getFieldValue());
        return new AnalysisDtos.ExtractedFieldResponse(info.getId(), info.getFieldName(), info.getFieldValue(), info.getConfidence(), info.getSourcePage(), info.getSourceExcerpt(), info.isValidated(), info.isIgnored(), conflict, current);
    }

    private String currentValue(String field, Tender tender) {
        return switch (field) {
            case "submissionDeadline" -> tender.getDeadline() == null ? null : tender.getDeadline().toString();
            case "publicationDate" -> tender.getPublicationDate() == null ? null : tender.getPublicationDate().toString();
            case "estimatedAmount" -> tender.getEstimatedAmount() == null ? null : tender.getEstimatedAmount().stripTrailingZeros().toPlainString();
            case "provisionalGuarantee" -> tender.getProvisionalGuarantee() == null ? null : tender.getProvisionalGuarantee().stripTrailingZeros().toPlainString();
            case "organization" -> tender.getOrganization();
            case "title" -> tender.getTitle();
            case "reference" -> tender.getReference();
            case "marketNumber" -> tender.getMarketNumber();
            default -> null;
        };
    }

    private AnalysisDtos.RequirementResponse toRequirement(ExtractedRequirement requirement) {
        return new AnalysisDtos.RequirementResponse(requirement.getId(), requirement.getCategory(), requirement.getLabel(), requirement.getDescription(), requirement.isRequired(), requirement.getSourcePage(), requirement.getConfidence(), requirement.getStatus());
    }

    private AnalysisDtos.ImportantDateResponse toImportantDate(ExtractedImportantDate date) {
        return new AnalysisDtos.ImportantDateResponse(date.getId(), date.getType(), date.getDetectedDate(), date.getLabel(), date.getSourcePage(), date.getConfidence());
    }

    private AnalysisDtos.SummaryResponse toSummary(DocumentSummary summary) {
        return new AnalysisDtos.SummaryResponse(summary.getShortSummary(), summary.getKeyPoints(), summary.getImportantDates(), summary.getRequiredDocuments(), summary.getFinancialConditions(), summary.getTechnicalConditions(), summary.getAttentionPoints());
    }
}
