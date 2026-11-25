package com.aomanagement.service;

import com.aomanagement.entity.*;
import com.aomanagement.repository.ExtractedImportantDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ImportantDateExtractionService {
    private final ExtractedImportantDateRepository dateRepository;
    private final TenderInformationExtractionService informationExtractionService;

    public void extract(DocumentAnalysis analysis, String text) {
        detect(text, "(date limite|remise des offres|depot des offres|dépôt des offres)[^\\n]{0,120}?([0-3]?\\d[/-][01]?\\d[/-]20\\d{2})", ImportantDateType.SUBMISSION_DEADLINE, "Depot des offres", analysis);
        detect(text, "(ouverture des plis|opening)[^\\n]{0,120}?([0-3]?\\d[/-][01]?\\d[/-]20\\d{2})", ImportantDateType.OPENING_DATE, "Ouverture des plis", analysis);
        detect(text, "(visite des lieux|visite obligatoire)[^\\n]{0,120}?([0-3]?\\d[/-][01]?\\d[/-]20\\d{2})", ImportantDateType.SITE_VISIT_DATE, "Visite des lieux", analysis);
        detect(text, "(questions|demande d.eclaircissement)[^\\n]{0,120}?([0-3]?\\d[/-][01]?\\d[/-]20\\d{2})", ImportantDateType.QUESTION_DEADLINE, "Date limite questions", analysis);
    }

    private void detect(String text, String regex, ImportantDateType type, String label, DocumentAnalysis analysis) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        if (matcher.find()) {
            LocalDate date = informationExtractionService.parseDate(matcher.group(2));
            if (date != null) {
                ExtractedImportantDate importantDate = new ExtractedImportantDate();
                importantDate.setAnalysis(analysis);
                importantDate.setType(type);
                importantDate.setDetectedDate(date);
                importantDate.setLabel(label);
                importantDate.setSourcePage(informationExtractionService.pageOf(text, matcher.start()));
                importantDate.setConfidence(BigDecimal.valueOf(0.84));
                dateRepository.save(importantDate);
            }
        }
    }
}
