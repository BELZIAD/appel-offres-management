package com.aomanagement.service;

import com.aomanagement.dto.AnalysisDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ConflictException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.*;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentAnalysisService {
    private final DocumentAnalysisRepository analysisRepository;
    private final DocumentVersionRepository versionRepository;
    private final TenderDocumentRepository documentRepository;
    private final ExtractedTenderInformationRepository informationRepository;
    private final ExtractedRequirementRepository requirementRepository;
    private final TenderChecklistItemRepository checklistItemRepository;
    private final TenderRepository tenderRepository;
    private final UserRepository userRepository;
    private final TenderService tenderService;
    private final MapperService mapperService;
    private final DocumentAnalysisProcessor processor;
    private final DocumentAnalysisMapper mapper;
    private final ActivityLogService activityLogService;
    private final TenderHistoryService historyService;

    @Transactional
    public AnalysisDtos.AnalysisResponse start(Long tenderId, Long documentId, UserPrincipal principal) {
        TenderDocument document = securedDocument(tenderId, documentId, principal);
        DocumentVersion version = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .orElseThrow(() -> new NotFoundException("Version document introuvable"));
        if (analysisRepository.existsByDocumentVersionIdAndStatusIn(version.getId(), List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))) {
            throw new ConflictException("Une analyse est deja en cours pour cette version");
        }
        DocumentAnalysis analysis = new DocumentAnalysis();
        analysis.setDocumentVersion(version);
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysis.setExtractionMethod(ExtractionMethod.FAILED);
        analysis.setDetectedLanguage("UNKNOWN");
        DocumentAnalysis saved = analysisRepository.save(analysis);
        User actor = currentUser(principal);
        activityLogService.record(actor, document.getTender(), "DOCUMENT_ANALYSIS", saved.getId(), "DOCUMENT_ANALYSIS_STARTED", actor.getFirstName() + " a lance l'analyse du document " + version.getOriginalFileName() + ".");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processor.processAsync(saved.getId(), principal.getId());
            }
        });
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AnalysisDtos.AnalysisResponse get(Long id, UserPrincipal principal) {
        DocumentAnalysis analysis = analysisRepository.findById(id).orElseThrow(() -> new NotFoundException("Analyse introuvable"));
        tenderService.findSecuredEntity(analysis.getDocumentVersion().getDocument().getTender().getId(), principal);
        return mapper.toResponse(analysis);
    }

    @Transactional(readOnly = true)
    public AnalysisDtos.AnalysisResponse latest(Long documentId, UserPrincipal principal) {
        TenderDocument document = documentRepository.findById(documentId).orElseThrow(() -> new NotFoundException("Document introuvable"));
        tenderService.findSecuredEntity(document.getTender().getId(), principal);
        return analysisRepository.findTopByDocumentVersionDocumentIdOrderByCreatedAtDesc(documentId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Aucune analyse pour ce document"));
    }

    @Transactional(readOnly = true)
    public List<AnalysisDtos.AnalysisResponse> byTender(Long tenderId, UserPrincipal principal) {
        tenderService.findSecuredEntity(tenderId, principal);
        return documentRepository.findByTenderIdOrderByUploadedAtDesc(tenderId).stream()
                .flatMap(document -> analysisRepository.findByDocumentVersionDocumentIdOrderByCreatedAtDesc(document.getId()).stream())
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public AnalysisDtos.AnalysisResponse applyField(Long tenderId, AnalysisDtos.ApplyFieldRequest request, UserPrincipal principal) {
        Tender tender = tenderService.findSecuredEntity(tenderId, principal);
        User actor = currentUser(principal);
        String oldValue = applyValue(tender, request.field(), request.value());
        tenderRepository.save(tender);
        if (request.extractedFieldId() != null) {
            ExtractedTenderInformation field = informationRepository.findById(request.extractedFieldId()).orElseThrow(() -> new NotFoundException("Champ extrait introuvable"));
            if (!field.getAnalysis().getDocumentVersion().getDocument().getTender().getId().equals(tenderId)) {
                throw new NotFoundException("Champ extrait introuvable");
            }
            field.setValidated(true);
            field.setIgnored(false);
        }
        historyService.record(tender, actor, TenderHistoryAction.UPDATED, request.field(), oldValue, request.value(), actor.getFirstName() + " a valide le champ extrait " + request.field() + ".");
        activityLogService.record(actor, tender, "EXTRACTED_FIELD", request.extractedFieldId(), "EXTRACTED_FIELD_APPLIED", actor.getFirstName() + " a applique une information extraite sur " + tender.getReference() + ".");
        if (request.extractedFieldId() != null) {
            ExtractedTenderInformation field = informationRepository.findById(request.extractedFieldId()).orElseThrow();
            return mapper.toResponse(field.getAnalysis());
        }
        throw new NotFoundException("Champ extrait introuvable");
    }

    @Transactional
    public List<com.aomanagement.dto.SprintFourDtos.ChecklistItemResponse> applyChecklist(Long tenderId, AnalysisDtos.ApplyChecklistRequest request, UserPrincipal principal) {
        Tender tender = tenderService.findSecuredEntity(tenderId, principal);
        User actor = currentUser(principal);
        DocumentAnalysis analysis = analysisRepository.findById(request.analysisId()).orElseThrow(() -> new NotFoundException("Analyse introuvable"));
        if (!analysis.getDocumentVersion().getDocument().getTender().getId().equals(tenderId)) {
            throw new NotFoundException("Analyse introuvable");
        }
        List<ExtractedRequirement> requirements = requirementRepository.findByAnalysisIdOrderByIdAsc(request.analysisId()).stream()
                .filter(r -> request.requirementIds() == null || request.requirementIds().isEmpty() || request.requirementIds().contains(r.getId()))
                .toList();
        Set<String> existing = new HashSet<>();
        List<TenderChecklistItem> current = checklistItemRepository.findByTenderIdOrderByPositionAsc(tenderId);
        current.forEach(item -> existing.add(normalizeLabel(item.getLabel())));
        int position = current.stream().mapToInt(TenderChecklistItem::getPosition).max().orElse(0) + 1;
        for (ExtractedRequirement requirement : requirements) {
            String key = normalizeLabel(requirement.getLabel());
            if (existing.contains(key)) continue;
            TenderChecklistItem item = new TenderChecklistItem();
            item.setTender(tender);
            item.setLabel(requirement.getLabel());
            item.setRequired(requirement.isRequired());
            item.setPosition(position++);
            item.setExpectedDocumentType(documentTypeFor(requirement.getLabel()));
            checklistItemRepository.save(item);
            requirement.setStatus(RequirementStatus.ACCEPTED);
            existing.add(key);
        }
        activityLogService.record(actor, tender, "CHECKLIST", tender.getId(), "AI_CHECKLIST_APPLIED", actor.getFirstName() + " a applique une checklist proposee pour " + tender.getReference() + ".");
        return checklistItemRepository.findByTenderIdOrderByPositionAsc(tenderId).stream().map(mapperService::toChecklistItemResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AnalysisDtos.DocumentSearchResult> searchDocument(Long documentId, String query, UserPrincipal principal) {
        TenderDocument document = documentRepository.findById(documentId).orElseThrow(() -> new NotFoundException("Document introuvable"));
        tenderService.findSecuredEntity(document.getTender().getId(), principal);
        DocumentAnalysis analysis = analysisRepository.findTopByDocumentVersionDocumentIdOrderByCreatedAtDesc(documentId)
                .orElseThrow(() -> new NotFoundException("Aucune analyse pour ce document"));
        String text = analysis.getNormalizedText() == null ? "" : analysis.getNormalizedText();
        if (query == null || query.isBlank()) return List.of();
        String lower = text.toLowerCase();
        String term = query.toLowerCase();
        List<AnalysisDtos.DocumentSearchResult> results = new ArrayList<>();
        int index = lower.indexOf(term);
        while (index >= 0 && results.size() < 10) {
            int start = Math.max(0, index - 80);
            int end = Math.min(text.length(), index + term.length() + 120);
            results.add(new AnalysisDtos.DocumentSearchResult(pageOf(text, index), text.substring(start, end).replaceAll("\\s+", " ").trim()));
            index = lower.indexOf(term, index + term.length());
        }
        return results;
    }

    private TenderDocument securedDocument(Long tenderId, Long documentId, UserPrincipal principal) {
        TenderDocument document = documentRepository.findById(documentId).orElseThrow(() -> new NotFoundException("Document introuvable"));
        if (!document.getTender().getId().equals(tenderId)) throw new NotFoundException("Document introuvable");
        tenderService.findSecuredEntity(tenderId, principal);
        return document;
    }

    private String applyValue(Tender tender, String field, String value) {
        return switch (field) {
            case "submissionDeadline" -> { String old = tender.getDeadline() == null ? null : tender.getDeadline().toString(); tender.setDeadline(LocalDate.parse(value)); yield old; }
            case "publicationDate" -> { String old = tender.getPublicationDate() == null ? null : tender.getPublicationDate().toString(); tender.setPublicationDate(LocalDate.parse(value)); yield old; }
            case "estimatedAmount" -> { String old = tender.getEstimatedAmount() == null ? null : tender.getEstimatedAmount().toPlainString(); tender.setEstimatedAmount(new BigDecimal(value)); yield old; }
            case "provisionalGuarantee" -> { String old = tender.getProvisionalGuarantee() == null ? null : tender.getProvisionalGuarantee().toPlainString(); tender.setProvisionalGuarantee(new BigDecimal(value)); yield old; }
            case "organization" -> { String old = tender.getOrganization(); tender.setOrganization(value); yield old; }
            case "title" -> { String old = tender.getTitle(); tender.setTitle(value); yield old; }
            case "marketNumber" -> { String old = tender.getMarketNumber(); tender.setMarketNumber(value); yield old; }
            default -> throw new IllegalArgumentException("Champ non applicable: " + field);
        };
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }

    private String normalizeLabel(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9àâçéèêëîïôûùüÿñæœ]+", " ").trim();
    }

    private DocumentType documentTypeFor(String label) {
        String lower = label == null ? "" : label.toLowerCase();
        if (lower.contains("registre") || lower.equals("rc")) return DocumentType.RC;
        if (lower.contains("technique") || lower.contains("memoire") || lower.contains("mémoire")) return DocumentType.TECHNICAL_DOCUMENT;
        if (lower.contains("financ") || lower.contains("prix") || lower.contains("caution")) return DocumentType.FINANCIAL_DOCUMENT;
        if (lower.contains("attestation")) return DocumentType.ADMINISTRATIVE_DOCUMENT;
        return null;
    }

    private int pageOf(String text, int offset) {
        int page = 1;
        Matcher matcher = Pattern.compile("--- PAGE (\\d+) ---").matcher(text.substring(0, Math.max(0, offset)));
        while (matcher.find()) page = Integer.parseInt(matcher.group(1));
        return page;
    }

}
