package com.aomanagement.controller;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.Priority;
import com.aomanagement.entity.TenderResult;
import com.aomanagement.entity.TenderStatus;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.TenderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/tenders")
@RequiredArgsConstructor
public class AdminTenderController {
    private final TenderService tenderService;

    @GetMapping
    public Page<TenderDtos.TenderResponse> all(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TenderStatus status,
            @RequestParam(required = false) TenderResult result,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String organization,
            @RequestParam(required = false) LocalDate deadlineFrom,
            @RequestParam(required = false) LocalDate deadlineTo,
            Pageable pageable
    ) {
        return tenderService.search(new TenderDtos.TenderSearchCriteria(search, status, result, priority, employeeId, organization, deadlineFrom, deadlineTo), pageable);
    }

    @PostMapping
    public TenderDtos.TenderResponse create(@Valid @RequestBody TenderDtos.TenderRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.create(request, principal);
    }

    @PutMapping("/{id}")
    public TenderDtos.TenderResponse update(@PathVariable Long id, @Valid @RequestBody TenderDtos.TenderRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.update(id, request, principal);
    }

    @PatchMapping("/{id}/assign/{employeeId}")
    public TenderDtos.TenderResponse assign(@PathVariable Long id, @PathVariable Long employeeId, @AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.assign(id, employeeId, principal);
    }

    @PatchMapping("/{id}/status")
    public TenderDtos.TenderResponse status(@PathVariable Long id, @Valid @RequestBody TenderDtos.StatusUpdateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.updateAdminStatus(id, request.status(), principal);
    }

    @PatchMapping("/{id}/result")
    public TenderDtos.TenderResponse result(@PathVariable Long id, @Valid @RequestBody TenderDtos.ResultUpdateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.updateResult(id, request.result(), request.finalAmount(), request.lossReason(), principal);
    }

    @PostMapping("/{id}/duplicate")
    public TenderDtos.TenderResponse duplicate(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return tenderService.duplicate(id, principal);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        tenderService.delete(id, principal);
    }
}
