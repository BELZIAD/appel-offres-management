package com.aomanagement.service;

import com.aomanagement.entity.Tender;
import com.aomanagement.entity.TenderAssignmentRole;
import com.aomanagement.repository.TenderAssignmentRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenderAuthorizationService {
    private final TenderAssignmentRepository assignmentRepository;

    public boolean canViewTender(UserPrincipal user, Tender tender) {
        return isAdmin(user) || legacyAssigned(user, tender) || assignmentRepository.existsByTenderIdAndEmployeeIdAndActiveTrue(tender.getId(), user.getId());
    }

    public boolean canManageTender(UserPrincipal user, Tender tender) {
        return isAdmin(user) || hasRole(user, tender, TenderAssignmentRole.TENDER_MANAGER);
    }

    public boolean canEditPricing(UserPrincipal user, Tender tender) {
        return isAdmin(user) || hasRole(user, tender, TenderAssignmentRole.PRICING_MANAGER);
    }

    public boolean canSubmitTender(UserPrincipal user, Tender tender) {
        return isAdmin(user) || hasRole(user, tender, TenderAssignmentRole.SUBMISSION_MANAGER);
    }

    public boolean canManageDocuments(UserPrincipal user, Tender tender) {
        return isAdmin(user) || hasRole(user, tender, TenderAssignmentRole.TENDER_MANAGER) || hasRole(user, tender, TenderAssignmentRole.SUBMISSION_MANAGER);
    }

    public boolean hasRole(UserPrincipal user, Tender tender, TenderAssignmentRole role) {
        return assignmentRepository.existsByTenderIdAndEmployeeIdAndAssignmentRoleAndActiveTrue(tender.getId(), user.getId(), role);
    }

    public boolean isAdmin(UserPrincipal user) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean legacyAssigned(UserPrincipal user, Tender tender) {
        return tender.getAssignedEmployee() != null && tender.getAssignedEmployee().getId().equals(user.getId());
    }
}
