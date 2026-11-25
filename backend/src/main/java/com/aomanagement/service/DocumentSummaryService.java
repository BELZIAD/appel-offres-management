package com.aomanagement.service;

import com.aomanagement.entity.DocumentAnalysis;
import com.aomanagement.entity.DocumentSummary;
import com.aomanagement.repository.DocumentSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentSummaryService {
    private final DocumentSummaryRepository summaryRepository;

    public void summarize(DocumentAnalysis analysis, String text) {
        DocumentSummary summary = new DocumentSummary();
        summary.setAnalysis(analysis);
        summary.setShortSummary(firstSentences(text, 2));
        summary.setKeyPoints(linesContaining(text, "objet", "date limite", "montant", "caution"));
        summary.setImportantDates(linesContaining(text, "date", "ouverture", "visite", "questions"));
        summary.setRequiredDocuments(linesContaining(text, "attestation", "registre", "caution", "memoire", "mémoire", "bordereau"));
        summary.setFinancialConditions(linesContaining(text, "montant", "caution", "prix", "financiere", "financière"));
        summary.setTechnicalConditions(linesContaining(text, "technique", "delai", "délai", "execution", "exécution"));
        summary.setAttentionPoints(linesContaining(text, "obligatoire", "caution", "visite", "date limite", "certification"));
        summaryRepository.save(summary);
    }

    private String firstSentences(String text, int max) {
        String clean = text.replaceAll("--- PAGE \\d+ ---", " ").replaceAll("\\s+", " ").trim();
        if (clean.isBlank()) return "Analyse avancee indisponible: aucun texte exploitable n'a ete extrait.";
        String[] parts = clean.split("(?<=[.!?])\\s+");
        return Arrays.stream(parts).limit(max).collect(Collectors.joining(" "));
    }

    private String linesContaining(String text, String... terms) {
        return Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> line.length() > 8)
                .filter(line -> {
                    String lower = line.toLowerCase();
                    return Arrays.stream(terms).anyMatch(lower::contains);
                })
                .limit(6)
                .collect(Collectors.joining("\n"));
    }
}
