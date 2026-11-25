package com.aomanagement.controller;

import com.aomanagement.dto.AssignmentDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.TenderAssignmentService;
import com.aomanagement.service.TenderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TenderAssignmentController {
    private final TenderAssignmentService service;
    private final TenderService tenderService;

    @GetMapping("/api/tenders/{id}/team")
    public AssignmentDtos.TeamResponse team(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        tenderService.findSecuredEntity(id, principal); // verifie que l'utilisateur a acces a cet AO
        return service.team(id);
    }

    @PostMapping("/api/admin/tenders/{id}/assignments")
    public AssignmentDtos.AssignmentResponse assign(@PathVariable Long id, @Valid @RequestBody AssignmentDtos.AssignmentRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return service.assign(id, request, principal);
    }

    @DeleteMapping("/api/admin/tenders/{id}/assignments/{assignmentId}")
    public void deactivate(@PathVariable Long id, @PathVariable Long assignmentId, @AuthenticationPrincipal UserPrincipal principal) {
        service.deactivate(id, assignmentId, principal);
    }
}
