package com.aomanagement.controller;

import com.aomanagement.dto.PartnerDtos;
import com.aomanagement.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PartnerController {
    private final PartnerService service;
    private final com.aomanagement.service.TenderService tenderService;

    @GetMapping("/api/admin/partners")
    public List<PartnerDtos.PartnerResponse> all(@RequestParam(required = false) String search) {
        return service.all(search);
    }

    @GetMapping("/api/admin/partners/{id}")
    public PartnerDtos.PartnerResponse one(@PathVariable Long id) {
        return service.one(id);
    }

    @PostMapping("/api/admin/partners")
    public PartnerDtos.PartnerResponse create(@Valid @RequestBody PartnerDtos.PartnerRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/admin/partners/{id}")
    public PartnerDtos.PartnerResponse update(@PathVariable Long id, @Valid @RequestBody PartnerDtos.PartnerRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/api/admin/partners/{id}/status")
    public PartnerDtos.PartnerResponse status(@PathVariable Long id, @RequestBody PartnerDtos.PartnerStatusRequest request) {
        return service.status(id, request);
    }

    @DeleteMapping("/api/admin/partners/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/api/tenders/{id}/participations")
    public PartnerDtos.ParticipationListResponse participations(@PathVariable Long id, @org.springframework.security.core.annotation.AuthenticationPrincipal com.aomanagement.security.UserPrincipal principal) {
        tenderService.findSecuredEntity(id, principal); // verifie que l'utilisateur a acces a cet AO
        return service.tenderParticipations(id);
    }

    @PostMapping("/api/admin/tenders/{id}/participations")
    public PartnerDtos.ParticipationResponse addParticipation(@PathVariable Long id, @Valid @RequestBody PartnerDtos.ParticipationRequest request) {
        return service.addParticipation(id, request);
    }

    @DeleteMapping("/api/admin/tenders/{id}/participations/{participationId}")
    public void removeParticipation(@PathVariable Long id, @PathVariable Long participationId) {
        service.removeParticipation(id, participationId);
    }
}
