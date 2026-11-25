package com.aomanagement.service;

import com.aomanagement.dto.AssignmentDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.ConflictException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.TenderAssignmentRepository;
import com.aomanagement.repository.TenderPricingRepository;
import com.aomanagement.repository.TenderRepository;
import com.aomanagement.repository.TenderSubmissionRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenderAssignmentService {
    private final TenderAssignmentRepository repository;
    private final TenderPricingRepository pricingRepository;
    private final TenderSubmissionRepository submissionRepository;
    private final TenderRepository tenderRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final NotificationService notificationService;
    private final TenderHistoryService historyService;

    @Transactional(readOnly = true)
    public AssignmentDtos.TeamResponse team(Long tenderId) {
        return new AssignmentDtos.TeamResponse(repository.findByTenderIdAndActiveTrueOrderByAssignmentRoleAscEmployeeLastNameAsc(tenderId).stream().map(this::toResponse).toList());
    }

    @Transactional
    public AssignmentDtos.AssignmentResponse assign(Long tenderId, AssignmentDtos.AssignmentRequest request, UserPrincipal principal) {
        Tender tender = tenderRepository.findById(tenderId).orElseThrow(() -> new NotFoundException("Appel d'offres introuvable"));
        User employee = userService.findEmployee(request.employeeId());
        User actor = userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        if (repository.existsByTenderIdAndEmployeeIdAndAssignmentRoleAndActiveTrue(tenderId, employee.getId(), request.assignmentRole())) {
            throw new ConflictException("Cette affectation existe deja");
        }
        TenderAssignment assignment = new TenderAssignment();
        assignment.setTender(tender);
        assignment.setEmployee(employee);
        assignment.setAssignmentRole(request.assignmentRole());
        assignment.setAssignedBy(actor);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setActive(request.active() == null || request.active());
        TenderAssignment saved = repository.save(assignment);
        if (request.assignmentRole() == TenderAssignmentRole.TENDER_MANAGER && tender.getAssignedEmployee() == null) {
            tender.setAssignedEmployee(employee);
        }
        if (request.assignmentRole() == TenderAssignmentRole.PRICING_MANAGER) {
            TenderPricing pricing = pricingRepository.findByTenderId(tenderId).orElseGet(() -> {
                TenderPricing created = new TenderPricing();
                created.setTender(tender);
                return pricingRepository.save(created);
            });
            pricing.setAssignedPricingManager(employee);
        }
        if (request.assignmentRole() == TenderAssignmentRole.SUBMISSION_MANAGER) {
            TenderSubmission submission = submissionRepository.findByTenderId(tenderId).orElseGet(() -> {
                TenderSubmission created = new TenderSubmission();
                created.setTender(tender);
                return submissionRepository.save(created);
            });
            submission.setSubmissionManager(employee);
        }
        notifyRole(tender, employee, request.assignmentRole());
        historyService.record(tender, actor, TenderHistoryAction.ASSIGNED, "assignmentRole", null, request.assignmentRole().name(), actor.getFirstName() + " a affecte " + employee.getFirstName() + " comme " + request.assignmentRole() + ".");
        return toResponse(saved);
    }

    /**
     * Remplace le responsable principal d'un AO : l'ancien responsable perd son affectation
     * TENDER_MANAGER (et donc l'acces a l'AO s'il n'a pas d'autre role), le nouveau l'obtient.
     */
    @Transactional
    public void replaceTenderManager(Tender tender, User oldEmployee, User newEmployee, UserPrincipal principal) {
        if (oldEmployee != null && (newEmployee == null || !oldEmployee.getId().equals(newEmployee.getId()))) {
            repository.findByTenderIdAndEmployeeIdAndAssignmentRoleAndActiveTrue(tender.getId(), oldEmployee.getId(), TenderAssignmentRole.TENDER_MANAGER)
                    .ifPresent(a -> a.setActive(false));
        }
        if (newEmployee != null && !repository.existsByTenderIdAndEmployeeIdAndAssignmentRoleAndActiveTrue(tender.getId(), newEmployee.getId(), TenderAssignmentRole.TENDER_MANAGER)) {
            assign(tender.getId(), new AssignmentDtos.AssignmentRequest(newEmployee.getId(), TenderAssignmentRole.TENDER_MANAGER, true), principal);
        }
    }

    @Transactional
    public void deactivate(Long tenderId, Long assignmentId, UserPrincipal principal) {
        TenderAssignment assignment = repository.findById(assignmentId).orElseThrow(() -> new NotFoundException("Affectation introuvable"));
        if (!assignment.getTender().getId().equals(tenderId)) throw new NotFoundException("Affectation introuvable");
        assignment.setActive(false);
        Tender tender = assignment.getTender();
        User employee = assignment.getEmployee();
        // Nettoyer les references "legacy" pour que la personne retiree perde vraiment l'acces / le role.
        if (assignment.getAssignmentRole() == TenderAssignmentRole.TENDER_MANAGER && tender.getAssignedEmployee() != null && tender.getAssignedEmployee().getId().equals(employee.getId())) {
            tender.setAssignedEmployee(repository.findByTenderIdAndActiveTrueOrderByAssignmentRoleAscEmployeeLastNameAsc(tenderId).stream()
                    .filter(a -> a.getAssignmentRole() == TenderAssignmentRole.TENDER_MANAGER && !a.getId().equals(assignment.getId()))
                    .map(TenderAssignment::getEmployee).findFirst().orElse(null));
        }
        if (assignment.getAssignmentRole() == TenderAssignmentRole.PRICING_MANAGER) {
            pricingRepository.findByTenderId(tenderId).filter(p -> p.getAssignedPricingManager() != null && p.getAssignedPricingManager().getId().equals(employee.getId()))
                    .ifPresent(p -> p.setAssignedPricingManager(null));
        }
        if (assignment.getAssignmentRole() == TenderAssignmentRole.SUBMISSION_MANAGER) {
            submissionRepository.findByTenderId(tenderId).filter(sub -> sub.getSubmissionManager() != null && sub.getSubmissionManager().getId().equals(employee.getId()))
                    .ifPresent(sub -> sub.setSubmissionManager(null));
        }
        User actor = userRepository.findById(principal.getId()).orElse(null);
        historyService.record(assignment.getTender(), actor, TenderHistoryAction.UPDATED, "assignment", assignment.getEmployee().getEmail(), null, "Affectation desactivee.");
    }

    public List<TenderAssignment> employeeAssignments(Long employeeId, TenderAssignmentRole role) {
        return role == null
                ? repository.findByEmployeeIdAndActiveTrueOrderByTenderDeadlineAsc(employeeId)
                : repository.findByEmployeeIdAndAssignmentRoleAndActiveTrueOrderByTenderDeadlineAsc(employeeId, role);
    }

    public List<TenderAssignment> tenderAssignments(Long tenderId) {
        return repository.findByTenderIdAndActiveTrueOrderByAssignmentRoleAscEmployeeLastNameAsc(tenderId);
    }

    private AssignmentDtos.AssignmentResponse toResponse(TenderAssignment assignment) {
        return new AssignmentDtos.AssignmentResponse(assignment.getId(), mapperService.toSimpleUser(assignment.getEmployee()), assignment.getAssignmentRole(), mapperService.toSimpleUser(assignment.getAssignedBy()), assignment.getAssignedAt(), assignment.isActive());
    }

    private void notifyRole(Tender tender, User employee, TenderAssignmentRole role) {
        String message = switch (role) {
            case TENDER_MANAGER -> "Vous etes responsable de l'AO " + tender.getReference() + ".";
            case PRICING_MANAGER -> "Vous etes responsable du chiffrage de " + tender.getReference() + ".";
            case SUBMISSION_MANAGER -> "Vous etes responsable du depot de " + tender.getReference() + ".";
            case CONTRIBUTOR -> "Vous etes collaborateur sur " + tender.getReference() + ".";
        };
        notificationService.create(employee, tender, NotificationType.TENDER_ASSIGNED, "Nouvelle responsabilite", message);
    }
}
