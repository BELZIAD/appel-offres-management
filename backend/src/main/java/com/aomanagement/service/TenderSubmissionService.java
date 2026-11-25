package com.aomanagement.service;

import com.aomanagement.dto.SubmissionDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.exception.InvalidUploadException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.TenderRepository;
import com.aomanagement.repository.TenderSubmissionRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenderSubmissionService {
    private final TenderRepository tenderRepository;
    private final TenderSubmissionRepository repository;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final TenderAuthorizationService authorizationService;
    private final TenderAssignmentService assignmentService;
    private final TenderPricingService pricingService;
    private final TenderHistoryService historyService;
    private final AssetStorageService assetStorageService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public SubmissionDtos.SubmissionResponse get(Long tenderId, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canViewTender(principal, tender)) throw new ForbiddenOperationException("Acces depot interdit");
        return toResponse(ensureSubmission(tender));
    }

    @Transactional
    public SubmissionDtos.SubmissionResponse updateStatus(Long tenderId, SubmissionDtos.SubmissionStatusRequest request, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canSubmitTender(principal, tender)) throw new ForbiddenOperationException("Modification depot interdite");
        TenderSubmission submission = ensureSubmission(tender);
        submission.setStatus(request.status());
        String action = switch (request.status()) {
            case IN_PROGRESS -> "SUBMISSION_STARTED";
            case FAILED -> "SUBMISSION_FAILED";
            case SUBMITTED -> "SUBMISSION_COMPLETED";
            case CONFIRMED -> "SUBMISSION_CONFIRMED";
            default -> "SUBMISSION_UPDATED";
        };
        historyService.record(tender, currentUser(principal), TenderHistoryAction.UPDATED, "submission", null, request.status().name(), action);
        syncTenderStatus(tender, submission.getStatus(), principal);
        return toResponse(submission);
    }

    @Transactional
    public SubmissionDtos.SubmissionResponse confirm(Long tenderId, SubmissionDtos.SubmissionConfirmRequest request, MultipartFile receipt, UserPrincipal principal) {
        Tender tender = tender(tenderId);
        if (!authorizationService.canSubmitTender(principal, tender)) throw new ForbiddenOperationException("Confirmation depot interdite");
        TenderSubmission submission = ensureSubmission(tender);
        submission.setPlatform(request.platform());
        submission.setSubmissionDate(request.submissionDate() == null ? LocalDate.now() : request.submissionDate());
        submission.setSubmissionTime(request.submissionTime());
        submission.setExternalReference(request.externalReference());
        submission.setNotes(request.notes());
        submission.setSubmittedBy(currentUser(principal));
        submission.setStatus(request.status() == null ? SubmissionStatus.CONFIRMED : request.status());
        if (receipt != null && !receipt.isEmpty()) {
            submission.setReceiptFilePath(storeReceipt(tenderId, receipt));
            historyService.record(tender, currentUser(principal), TenderHistoryAction.DOCUMENT_ADDED, "receipt", null, receipt.getOriginalFilename(), "SUBMISSION_RECEIPT_ADDED");
        }
        historyService.record(tender, currentUser(principal), TenderHistoryAction.UPDATED, "submission", null, submission.getStatus().name(), "SUBMISSION_CONFIRMED " + (submission.getExternalReference() == null ? "" : submission.getExternalReference()));
        syncTenderStatus(tender, submission.getStatus(), principal);
        return toResponse(submission);
    }

    /** Un depot effectue fait passer l'AO au statut "Depose" (sinon il restait "En cours" dans les tableaux de bord). */
    private void syncTenderStatus(Tender tender, SubmissionStatus submissionStatus, UserPrincipal principal) {
        if ((submissionStatus == SubmissionStatus.SUBMITTED || submissionStatus == SubmissionStatus.CONFIRMED)
                && tender.getStatus() != TenderStatus.SUBMITTED && tender.getStatus() != TenderStatus.CLOSED) {
            TenderStatus old = tender.getStatus();
            tender.setStatus(TenderStatus.SUBMITTED);
            historyService.record(tender, currentUser(principal), TenderHistoryAction.STATUS_CHANGED, "status", old.name(), TenderStatus.SUBMITTED.name(), "Statut passe a SUBMITTED suite a la confirmation du depot.");
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<SubmissionDtos.SubmissionResponse> mySubmissions(Long employeeId) {
        return repository.findBySubmissionManagerIdOrderByTenderDeadlineAsc(employeeId).stream().map(this::toResponse).toList();
    }

    public Resource receipt(Long tenderId, UserPrincipal principal) {
        TenderSubmission submission = repository.findByTenderId(tenderId).orElseThrow(() -> new NotFoundException("Depot introuvable"));
        if (!authorizationService.canViewTender(principal, submission.getTender())) throw new ForbiddenOperationException("Acces recu interdit");
        if (submission.getReceiptFilePath() == null) throw new NotFoundException("Recu introuvable");
        String name = Path.of(submission.getReceiptFilePath()).getFileName().toString();
        return assetStorageService.load("submissions/" + tenderId, name);
    }

    public TenderSubmission ensureSubmission(Tender tender) {
        return repository.findByTenderId(tender.getId()).orElseGet(() -> {
            TenderSubmission submission = new TenderSubmission();
            submission.setTender(tender);
            submission.setStatus(SubmissionStatus.NOT_READY);
            return repository.save(submission);
        });
    }

    private SubmissionDtos.SubmissionResponse toResponse(TenderSubmission submission) {
        Tender tender = submission.getTender();
        boolean pricingValidated = pricingService.ensurePricing(tender).getStatus() == PricingStatus.VALIDATED;
        boolean checklistComplete = true;
        boolean managerAssigned = submission.getSubmissionManager() != null;
        SubmissionReadiness readiness = pricingValidated && checklistComplete && managerAssigned ? SubmissionReadiness.READY : (pricingValidated || managerAssigned ? SubmissionReadiness.ATTENTION : SubmissionReadiness.NOT_READY);
        return new SubmissionDtos.SubmissionResponse(submission.getId(), tender.getId(), mapperService.toSimpleUser(submission.getSubmissionManager()), submission.getPlatform(), submission.getStatus(), submission.getSubmissionDate(), submission.getSubmissionTime(), submission.getExternalReference(), submission.getReceiptFilePath(), submission.getNotes(), mapperService.toSimpleUser(submission.getSubmittedBy()), readiness, pricingValidated, checklistComplete, managerAssigned, submission.getCreatedAt(), submission.getUpdatedAt());
    }

    private String storeReceipt(Long tenderId, MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "receipt.pdf" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".pdf")) throw new InvalidUploadException("Le recu doit etre un PDF");
        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize();
            Path dir = root.resolve("submissions").resolve(String.valueOf(tenderId)).normalize();
            if (!dir.startsWith(root)) throw new InvalidUploadException("Chemin invalide");
            Files.createDirectories(dir);
            String stored = UUID.randomUUID() + ".pdf";
            Path target = dir.resolve(stored).normalize();
            file.transferTo(target);
            return target.toString();
        } catch (Exception ex) {
            throw new InvalidUploadException("Upload recu impossible");
        }
    }

    private Tender tender(Long id) { return tenderRepository.findById(id).orElseThrow(() -> new NotFoundException("Appel d'offres introuvable")); }
    private User currentUser(UserPrincipal principal) { return userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable")); }
}
