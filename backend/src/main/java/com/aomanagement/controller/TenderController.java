package com.aomanagement.controller;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.TenderHistoryService;
import com.aomanagement.service.TenderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TenderController {
    private final TenderService tenderService;
    private final TenderHistoryService historyService;

    @GetMapping("/api/tenders/{id}")
    public TenderDtos.TenderResponse one(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.getSecured(id, principal);
    }

    @GetMapping("/api/tenders/{id}/history")
    public Page<TenderDtos.HistoryResponse> history(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        tenderService.findSecuredEntity(id, principal);
        return historyService.forTender(id, pageable);
    }

    @GetMapping("/api/employee/tenders")
    public List<TenderDtos.TenderResponse> employeeTenders(@AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.assignedTo(principal.getId());
    }

    @PatchMapping("/api/employee/tenders/{id}/status")
    public TenderDtos.TenderResponse employeeStatus(@PathVariable Long id, @Valid @RequestBody TenderDtos.StatusUpdateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.updateEmployeeStatus(id, request.status(), principal);
    }

    @GetMapping("/api/employee/dashboard")
    public TenderDtos.EmployeeDashboardStats employeeDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.employeeStats(principal.getId());
    }

    @GetMapping("/api/admin/dashboard")
    public TenderDtos.DashboardStats adminDashboard() {
        return tenderService.adminStats();
    }
}
