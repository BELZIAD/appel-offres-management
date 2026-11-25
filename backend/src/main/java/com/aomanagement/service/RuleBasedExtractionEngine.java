package com.aomanagement.service;

import com.aomanagement.entity.DocumentAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RuleBasedExtractionEngine implements TenderExtractionEngine {
    private final TenderInformationExtractionService informationExtractionService;
    private final RequirementExtractionService requirementExtractionService;
    private final ImportantDateExtractionService importantDateExtractionService;
    private final DocumentSummaryService documentSummaryService;

    @Override
    public String version() {
        return "RULES_V1";
    }

    @Override
    public void extract(DocumentAnalysis analysis, String normalizedText) {
        informationExtractionService.extract(analysis, normalizedText);
        requirementExtractionService.extract(analysis, normalizedText);
        importantDateExtractionService.extract(analysis, normalizedText);
        documentSummaryService.summarize(analysis, normalizedText);
    }
}
