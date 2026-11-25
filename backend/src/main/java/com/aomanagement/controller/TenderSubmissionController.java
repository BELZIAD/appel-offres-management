package com.aomanagement.controller;

import com.aomanagement.dto.SubmissionDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.TenderSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TenderSubmissionController {
    private final TenderSubmissionService service;

    @GetMapping("/api/tenders/{id}/submission")
    public SubmissionDtos.SubmissionResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.get(id, principal);
    }

    @PatchMapping("/api/tenders/{id}/submission/status")
    public SubmissionDtos.SubmissionResponse status(@PathVariable Long id, @Valid @RequestBody SubmissionDtos.SubmissionStatusRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return service.updateStatus(id, request, principal);
    }

    @PostMapping("/api/tenders/{id}/submission/confirm")
    public SubmissionDtos.SubmissionResponse confirm(@PathVariable Long id, @Valid @RequestPart("data") SubmissionDtos.SubmissionConfirmRequest request, @RequestPart(value = "receipt", required = false) MultipartFile receipt, @AuthenticationPrincipal UserPrincipal principal) {
        return service.confirm(id, request, receipt, principal);
    }

    @GetMapping("/api/tenders/{id}/submission/receipt")
    public Resource receipt(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.receipt(id, principal);
    }

    @GetMapping("/api/employee/submissions")
    public List<SubmissionDtos.SubmissionResponse> mySubmissions(@AuthenticationPrincipal UserPrincipal principal) {
        return service.mySubmissions(principal.getId());
    }
}
