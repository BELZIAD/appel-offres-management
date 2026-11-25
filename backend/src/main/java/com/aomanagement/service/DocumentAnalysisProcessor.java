package com.aomanagement.service;

import com.aomanagement.dto.AnalysisDtos;
import com.aomanagement.entity.*;
import com.aomanagement.repository.DocumentAnalysisRepository;
import com.aomanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisProcessor {
    private final DocumentAnalysisRepository analysisRepository;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final OcrService ocrService;
    private final DocumentClassificationService classificationService;
    private final TenderExtractionEngine extractionEngine;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    @Value("${app.analysis.max-pages:60}")
    private int maxPages;

    @Value("${app.analysis.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    @Async
    @Transactional
    public void processAsync(Long analysisId, Long actorId) {
        process(analysisId, actorId);
    }

    @Transactional
    public void process(Long analysisId, Long actorId) {
        long started = System.currentTimeMillis();
        DocumentAnalysis analysis = analysisRepository.findById(analysisId).orElseThrow();
        Tender tender = analysis.getDocumentVersion().getDocument().getTender();
        User actor = actorId == null ? null : userRepository.findById(actorId).orElse(null);
        try {
            analysis.setStatus(AnalysisStatus.PROCESSING);
            analysis.setStartedAt(LocalDateTime.now());
            Path file = Path.of(analysis.getDocumentVersion().getFilePath()).normalize();
            if (analysis.getDocumentVersion().getFileSize() > maxFileSizeBytes) {
                throw new IllegalStateException("Document trop volumineux pour l'analyse configuree");
            }
            AnalysisDtos.PdfExtractionResult extracted = pdfTextExtractionService.extractText(file);
            if (extracted.pageCount() > maxPages) {
                throw new IllegalStateException("Document trop long pour l'analyse configuree");
            }
            if (extracted.requiresOcr() && ocrService.isAvailable()) {
                extracted = ocrService.extractText(file);
            }
            analysis.setExtractionMethod(extracted.method());
            analysis.setPageCount(extracted.pageCount());
            analysis.setTextLength(extracted.textLength());
            analysis.setRequiresOcr(extracted.requiresOcr());
            analysis.setRawText(extracted.text());
            analysis.setNormalizedText(normalize(extracted.text()));
            analysis.setDetectedLanguage(classificationService.detectLanguage(extracted.text()));
            analysis.setAnalysisEngineVersion(extractionEngine.version());
            if (extracted.method() == ExtractionMethod.FAILED) {
                throw new IllegalStateException("Extraction du texte PDF impossible");
            }
            if (!analysis.getNormalizedText().isBlank()) {
                extractionEngine.extract(analysis, analysis.getNormalizedText());
            }
            analysis.setStatus(extracted.requiresOcr() && !ocrService.isAvailable() ? AnalysisStatus.REQUIRES_REVIEW : AnalysisStatus.COMPLETED);
            analysis.setCompletedAt(LocalDateTime.now());
            notificationService.create(actor, tender, NotificationType.DOCUMENT_ANALYSIS_COMPLETED, "Analyse terminee", "L'analyse du document " + analysis.getDocumentVersion().getOriginalFileName() + " de " + tender.getReference() + " est terminee.");
            activityLogService.record(actor, tender, "DOCUMENT_ANALYSIS", analysis.getId(), "DOCUMENT_ANALYSIS_COMPLETED", "Analyse documentaire terminee pour " + tender.getReference() + ".");
            log.info("analysisId={} documentId={} tenderId={} status={} durationMs={} extractionMethod={}", analysis.getId(), analysis.getDocumentVersion().getDocument().getId(), tender.getId(), analysis.getStatus(), System.currentTimeMillis() - started, analysis.getExtractionMethod());
        } catch (Exception ex) {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setExtractionMethod(ExtractionMethod.FAILED);
            analysis.setErrorMessage(ex.getMessage());
            analysis.setCompletedAt(LocalDateTime.now());
            notificationService.create(actor, tender, NotificationType.DOCUMENT_ANALYSIS_FAILED, "Analyse echouee", "L'analyse du document " + analysis.getDocumentVersion().getOriginalFileName() + " a echoue.");
            activityLogService.record(actor, tender, "DOCUMENT_ANALYSIS", analysis.getId(), "DOCUMENT_ANALYSIS_FAILED", "Analyse documentaire echouee pour " + tender.getReference() + ".");
            log.warn("analysisId={} documentId={} tenderId={} status=FAILED durationMs={} error={}", analysis.getId(), analysis.getDocumentVersion().getDocument().getId(), tender.getId(), System.currentTimeMillis() - started, ex.getMessage());
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\u00A0', ' ').replaceAll("[\\t ]+", " ").replaceAll("\\R{3,}", "\n\n").trim();
    }
}
