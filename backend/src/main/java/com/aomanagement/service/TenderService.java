package com.aomanagement.service;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ConflictException;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.TenderRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TenderService {
    private final TenderRepository tenderRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MapperService mapperService;
    private final NotificationService notificationService;
    private final TenderHistoryService historyService;
    private final TenderAuthorizationService authorizationService;
    private final TenderAssignmentService assignmentService;
    private final com.aomanagement.repository.TenderChecklistItemRepository checklistItemRepository;

    @Transactional(readOnly = true)
    public Page<TenderDtos.TenderResponse> search(TenderDtos.TenderSearchCriteria criteria, Pageable pageable) {
        return tenderRepository.findAll(buildSpecification(criteria), pageable).map(t -> mapperService.toTenderResponse(t, false));
    }

    @Transactional(readOnly = true)
    public List<TenderDtos.TenderResponse> assignedTo(Long employeeId) {
        return assignmentService.employeeAssignments(employeeId, null).stream()
                .map(a -> a.getTender())
                .distinct()
                .map(t -> mapperService.toTenderResponse(t, false)).toList();
    }

    @Transactional(readOnly = true)
    public TenderDtos.TenderResponse getSecured(Long id, UserPrincipal principal) {
        Tender tender = find(id);
        assertTenderAccess(tender, principal);
        return mapperService.toTenderResponse(tender, true, principal.getId());
    }

    @Transactional
    public TenderDtos.TenderResponse create(TenderDtos.TenderRequest request, UserPrincipal principal) {
        if (tenderRepository.existsByReferenceIgnoreCase(request.reference())) {
            throw new ConflictException("Reference d'appel d'offres deja utilisee");
        }
        User actor = currentUser(principal);
        Tender tender = new Tender();
        apply(tender, request);
        tender.setCreatedBy(actor);
        Tender saved = tenderRepository.save(tender);
        historyService.record(saved, actor, TenderHistoryAction.CREATED, null, null, null, actor.getFirstName() + " a cree l'appel d'offres " + saved.getReference() + ".");
        if (saved.getAssignedEmployee() != null) {
            assignmentService.assign(saved.getId(), new com.aomanagement.dto.AssignmentDtos.AssignmentRequest(saved.getAssignedEmployee().getId(), TenderAssignmentRole.TENDER_MANAGER, true), principal);
        }
        return mapperService.toTenderResponse(saved, true, principal.getId());
    }

    @Transactional
    public TenderDtos.TenderResponse update(Long id, TenderDtos.TenderRequest request, UserPrincipal principal) {
        Tender tender = find(id);
        if (tenderRepository.existsByReferenceIgnoreCaseAndIdNot(request.reference(), id)) {
            throw new ConflictException("Reference d'appel d'offres deja utilisee");
        }
        User actor = currentUser(principal);
        TenderStatus oldStatus = tender.getStatus();
        TenderResult oldResult = tender.getResult();
        User oldEmployee = tender.getAssignedEmployee();
        Long oldEmployeeId = oldEmployee == null ? null : oldEmployee.getId();

        apply(tender, request);

        historyService.record(tender, actor, TenderHistoryAction.UPDATED, null, null, null, actor.getFirstName() + " a modifie " + tender.getReference() + ".");
        recordStatusIfChanged(tender, actor, oldStatus, tender.getStatus());
        recordResultIfChanged(tender, actor, oldResult, tender.getResult());
        Long newEmployeeId = tender.getAssignedEmployee() == null ? null : tender.getAssignedEmployee().getId();
        if (!Objects.equals(oldEmployeeId, newEmployeeId)) {
            assignmentService.replaceTenderManager(tender, oldEmployee, tender.getAssignedEmployee(), principal);
        }
        return mapperService.toTenderResponse(tender, true, principal.getId());
    }

    @Transactional
    public TenderDtos.TenderResponse assign(Long id, Long employeeId, UserPrincipal principal) {
        Tender tender = find(id);
        User actor = currentUser(principal);
        User oldEmployee = tender.getAssignedEmployee();
        User newEmployee = userService.findEmployee(employeeId);
        tender.setAssignedEmployee(newEmployee);
        if (!Objects.equals(oldEmployee == null ? null : oldEmployee.getId(), newEmployee.getId())) {
            assignmentService.replaceTenderManager(tender, oldEmployee, newEmployee, principal);
        }
        return mapperService.toTenderResponse(tender, true, principal.getId());
    }

    /**
     * Duplique un AO (utile pour les marches recurrents) : informations generales, montants,
     * priorite et checklist (decochee). Statut/resultat/documents ne sont pas copies.
     */
    @Transactional
    public TenderDtos.TenderResponse duplicate(Long id, UserPrincipal principal) {
        Tender source = find(id);
        User actor = currentUser(principal);
        String base = source.getReference() + "-COPIE";
        String reference = base;
        for (int i = 2; tenderRepository.existsByReferenceIgnoreCase(reference); i++) reference = base + "-" + i;
        Tender copy = new Tender();
        copy.setReference(reference);
        copy.setMarketNumber(source.getMarketNumber());
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setOrganization(source.getOrganization());
        copy.setPublicationDate(source.getPublicationDate());
        copy.setDeadline(source.getDeadline());
        copy.setEstimatedAmount(source.getEstimatedAmount());
        copy.setProvisionalGuarantee(source.getProvisionalGuarantee());
        copy.setPriority(source.getPriority());
        copy.setStatus(TenderStatus.NEW);
        copy.setResult(TenderResult.PENDING);
        copy.setCreatedBy(actor);
        Tender saved = tenderRepository.save(copy);
        for (TenderChecklistItem item : checklistItemRepository.findByTenderIdOrderByPositionAsc(source.getId())) {
            TenderChecklistItem clone = new TenderChecklistItem();
            clone.setTender(saved);
            clone.setLabel(item.getLabel());
            clone.setRequired(item.isRequired());
            clone.setPosition(item.getPosition());
            clone.setExpectedDocumentType(item.getExpectedDocumentType());
            checklistItemRepository.save(clone);
        }
        historyService.record(saved, actor, TenderHistoryAction.CREATED, null, null, null, actor.getFirstName() + " a cree " + saved.getReference() + " par duplication de " + source.getReference() + ".");
        return mapperService.toTenderResponse(saved, true, principal.getId());
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        Tender tender = find(id);
        historyService.record(tender, currentUser(principal), TenderHistoryAction.DELETED, null, null, null, "Appel d'offres supprime: " + tender.getReference() + ".");
        tenderRepository.delete(tender);
    }

    @Transactional(readOnly = true)
    public TenderDtos.DashboardStats adminStats() {
        LocalDate today = LocalDate.now();
        return new TenderDtos.DashboardStats(
                tenderRepository.count(),
                tenderRepository.countByStatus(TenderStatus.NEW),
                tenderRepository.countByStatus(TenderStatus.IN_PROGRESS),
                tenderRepository.countByStatus(TenderStatus.SUBMITTED),
                tenderRepository.countByResult(TenderResult.WON),
                tenderRepository.countByResult(TenderResult.LOST),
                tenderRepository.countByStatus(TenderStatus.READY_TO_SUBMIT),
                tenderRepository.countByDeadlineBetweenAndStatusNot(today.plusDays(1), today.plusDays(2), TenderStatus.CLOSED),
                tenderRepository.countByDeadlineBeforeAndStatusNot(today, TenderStatus.CLOSED),
                tenderRepository.countByDeadlineBetween(today, today.plusDays(7)),
                tenderRepository.findTop8ByOrderByCreatedAtDesc().stream().map(t -> mapperService.toTenderResponse(t, false)).toList(),
                tenderRepository.findTop5ByStatusNotAndDeadlineGreaterThanEqualOrderByDeadlineAsc(TenderStatus.CLOSED, today).stream().map(t -> mapperService.toTenderResponse(t, false)).toList()
        );
    }

    @Transactional(readOnly = true)
    public TenderDtos.EmployeeDashboardStats employeeStats(Long employeeId) {
        return new TenderDtos.EmployeeDashboardStats(
                tenderRepository.countByAssignedEmployeeId(employeeId),
                tenderRepository.countByAssignedEmployeeIdAndStatus(employeeId, TenderStatus.IN_PROGRESS),
                tenderRepository.countByAssignedEmployeeIdAndPriorityAndStatusNot(employeeId, Priority.URGENT, TenderStatus.CLOSED),
                tenderRepository.countByAssignedEmployeeIdAndStatus(employeeId, TenderStatus.READY_TO_SUBMIT),
                tenderRepository.countByAssignedEmployeeIdAndStatus(employeeId, TenderStatus.SUBMITTED),
                tenderRepository.countByAssignedEmployeeIdAndResult(employeeId, TenderResult.WON),
                tenderRepository.countByAssignedEmployeeIdAndResult(employeeId, TenderResult.LOST),
                tenderRepository.findTop5ByAssignedEmployeeIdAndStatusNotAndDeadlineGreaterThanEqualOrderByDeadlineAsc(employeeId, TenderStatus.CLOSED, LocalDate.now()).stream().map(t -> mapperService.toTenderResponse(t, false)).toList()
        );
    }

    @Transactional
    public TenderDtos.TenderResponse updateEmployeeStatus(Long id, TenderStatus status, UserPrincipal principal) {
        Tender tender = find(id);
        if (!authorizationService.canManageTender(principal, tender)) {
            throw new ForbiddenOperationException("Acces interdit a cet appel d'offres");
        }
        User actor = currentUser(principal);
        TenderStatus oldStatus = tender.getStatus();
        tender.setStatus(status);
        recordStatusIfChanged(tender, actor, oldStatus, status);
        return mapperService.toTenderResponse(tender, true, principal.getId());
    }

    @Transactional
    public TenderDtos.TenderResponse updateAdminStatus(Long id, TenderStatus status, UserPrincipal principal) {
        Tender tender = find(id);
        User actor = currentUser(principal);
        TenderStatus oldStatus = tender.getStatus();
        tender.setStatus(status);
        recordStatusIfChanged(tender, actor, oldStatus, status);
        return mapperService.toTenderResponse(tender, true, principal.getId());
    }

    @Transactional
    public TenderDtos.TenderResponse updateResult(Long id, TenderResult result, java.math.BigDecimal finalAmount, String lossReason, UserPrincipal principal) {
        Tender tender = find(id);
        User actor = currentUser(principal);
        TenderResult oldResult = tender.getResult();
        tender.setResult(result);
        if (result == TenderResult.WON) {
            tender.setFinalAmount(finalAmount);
            tender.setLossReason(null);
            if (oldResult == TenderResult.PENDING || tender.getResultDate() == null) {
                tender.setResultDate(LocalDate.now());
            }
        } else if (result == TenderResult.LOST) {
            tender.setFinalAmount(null);
            tender.setLossReason(lossReason);
            if (oldResult == TenderResult.PENDING || tender.getResultDate() == null) {
                tender.setResultDate(LocalDate.now());
            }
        } else if (result == TenderResult.CANCELLED) {
            tender.setFinalAmount(null);
            tender.setLossReason(null);
            if (oldResult == TenderResult.PENDING || tender.getResultDate() == null) {
                tender.setResultDate(LocalDate.now());
            }
        } else {
            tender.setFinalAmount(null);
            tender.setLossReason(null);
            tender.setResultDate(null);
        }
        recordResultIfChanged(tender, actor, oldResult, result);
        return mapperService.toTenderResponse(tender, true, principal.getId());
    }

    @Transactional(readOnly = true)
    public Tender findSecuredEntity(Long id, UserPrincipal principal) {
        Tender tender = find(id);
        assertTenderAccess(tender, principal);
        return tender;
    }

    public Tender find(Long id) {
        return tenderRepository.findById(id).orElseThrow(() -> new NotFoundException("Appel d'offres introuvable"));
    }

    private void apply(Tender tender, TenderDtos.TenderRequest request) {
        tender.setReference(request.reference());
        tender.setMarketNumber(request.marketNumber());
        tender.setTitle(request.title());
        tender.setDescription(request.description());
        tender.setOrganization(request.organization());
        tender.setPublicationDate(request.publicationDate());
        tender.setDeadline(request.deadline());
        tender.setEstimatedAmount(request.estimatedAmount());
        tender.setProvisionalGuarantee(request.provisionalGuarantee());
        tender.setFinalAmount(request.finalAmount());
        tender.setLossReason(request.lossReason());
        tender.setPriority(request.priority() == null ? Priority.NORMAL : request.priority());
        tender.setStatus(request.status() == null ? TenderStatus.NEW : request.status());
        tender.setResult(request.result() == null ? TenderResult.PENDING : request.result());
        tender.setAssignedEmployee(request.assignedEmployeeId() == null ? null : userService.findEmployee(request.assignedEmployeeId()));
    }

    private void notifyAssigned(Tender tender, User actor, User oldEmployee, User newEmployee) {
        notificationService.create(newEmployee, tender, NotificationType.TENDER_ASSIGNED, "Appel d'offres affecte", tender.getReference() + " vous a ete affecte.");
        historyService.record(tender, actor, oldEmployee == null ? TenderHistoryAction.ASSIGNED : TenderHistoryAction.REASSIGNED, "assignedEmployee", oldEmployee == null ? null : oldEmployee.getEmail(), newEmployee.getEmail(), "Admin a affecte " + tender.getReference() + " a " + newEmployee.getFirstName() + ".");
    }

    private void recordStatusIfChanged(Tender tender, User actor, TenderStatus oldStatus, TenderStatus newStatus) {
        if (oldStatus != newStatus) {
            notificationService.create(tender.getAssignedEmployee(), tender, NotificationType.STATUS_CHANGED, "Statut modifie", tender.getReference() + " est passe a " + newStatus + ".");
            historyService.record(tender, actor, TenderHistoryAction.STATUS_CHANGED, "status", oldStatus.name(), newStatus.name(), actor.getFirstName() + " a change le statut : " + oldStatus + " -> " + newStatus + ".");
        }
    }

    private void recordResultIfChanged(Tender tender, User actor, TenderResult oldResult, TenderResult newResult) {
        if (oldResult != newResult) {
            notificationService.create(tender.getAssignedEmployee(), tender, NotificationType.RESULT_CHANGED, "Resultat modifie", tender.getReference() + " est passe a " + newResult + ".");
            historyService.record(tender, actor, TenderHistoryAction.RESULT_CHANGED, "result", oldResult.name(), newResult.name(), actor.getFirstName() + " a change le resultat : " + oldResult + " -> " + newResult + ".");
        }
    }

    private void assertTenderAccess(Tender tender, UserPrincipal principal) {
        if (isEmployee(principal) && !authorizationService.canViewTender(principal, tender)) {
            throw new ForbiddenOperationException("Acces interdit a cet appel d'offres");
        }
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }

    public Specification<Tender> buildSpecification(TenderDtos.TenderSearchCriteria criteria) {
        Specification<Tender> spec = Specification.where(null);
        if (criteria == null) {
            return spec;
        }
        if (criteria.search() != null && !criteria.search().isBlank()) {
            String like = "%" + criteria.search().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("reference")), like),
                    cb.like(cb.lower(root.get("marketNumber")), like),
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("organization")), like)
            ));
        }
        if (criteria.status() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), criteria.status()));
        if (criteria.result() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("result"), criteria.result()));
        if (criteria.priority() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), criteria.priority()));
        if (criteria.employeeId() != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedEmployee").get("id"), criteria.employeeId()));
        if (criteria.organization() != null && !criteria.organization().isBlank()) spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("organization")), "%" + criteria.organization().toLowerCase() + "%"));
        if (criteria.deadlineFrom() != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("deadline"), criteria.deadlineFrom()));
        if (criteria.deadlineTo() != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("deadline"), criteria.deadlineTo()));
        return spec;
    }

    private boolean isEmployee(UserPrincipal principal) {
        return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }
}
