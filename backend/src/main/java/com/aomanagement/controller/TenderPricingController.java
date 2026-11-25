package com.aomanagement.controller;

import com.aomanagement.dto.PricingDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.TenderPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TenderPricingController {
    private final TenderPricingService service;

    @GetMapping("/api/tenders/{id}/pricing")
    public PricingDtos.PricingResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.get(id, principal);
    }

    @PostMapping("/api/tenders/{id}/pricing/lines")
    public PricingDtos.PricingLineResponse addLine(@PathVariable Long id, @Valid @RequestBody PricingDtos.PricingLineRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return service.addLine(id, request, principal);
    }

    @PutMapping("/api/tenders/{id}/pricing/lines/{lineId}")
    public PricingDtos.PricingLineResponse updateLine(@PathVariable Long id, @PathVariable Long lineId, @Valid @RequestBody PricingDtos.PricingLineRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return service.updateLine(id, lineId, request, principal);
    }

    @DeleteMapping("/api/tenders/{id}/pricing/lines/{lineId}")
    public void deleteLine(@PathVariable Long id, @PathVariable Long lineId, @AuthenticationPrincipal UserPrincipal principal) {
        service.deleteLine(id, lineId, principal);
    }

    @PatchMapping("/api/tenders/{id}/pricing/status")
    public PricingDtos.PricingResponse status(@PathVariable Long id, @Valid @RequestBody PricingDtos.PricingStatusRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return service.updateStatus(id, request, principal);
    }

    @PostMapping("/api/tenders/{id}/pricing/import")
    public PricingDtos.PricingImportPreview previewImport(@PathVariable Long id, @RequestParam MultipartFile file, @AuthenticationPrincipal UserPrincipal principal) {
        return service.previewImport(id, file, principal);
    }

    @PostMapping("/api/tenders/{id}/pricing/import/apply")
    public PricingDtos.PricingResponse applyImport(@PathVariable Long id, @RequestBody PricingDtos.PricingImportPreview preview, @RequestParam(defaultValue = "false") boolean replace, @AuthenticationPrincipal UserPrincipal principal) {
        return service.applyImport(id, preview, replace, principal);
    }

    @GetMapping("/api/tenders/{id}/pricing/export")
    public ResponseEntity<ByteArrayResource> export(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.export(id, principal);
    }

    @GetMapping("/api/employee/pricing")
    public List<PricingDtos.PricingResponse> myPricing(@AuthenticationPrincipal UserPrincipal principal) {
        return service.myPricing(principal.getId());
    }
}
