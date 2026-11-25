package com.aomanagement.controller;

import com.aomanagement.dto.AnalysisDtos;
import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.DocumentAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentAnalysisController {
    private final DocumentAnalysisService analysisService;

    @PostMapping("/api/tenders/{tenderId}/documents/{documentId}/analyze")
    public AnalysisDtos.AnalysisResponse analyze(@PathVariable Long tenderId, @PathVariable Long documentId, @AuthenticationPrincipal UserPrincipal principal) {
        return analysisService.start(tenderId, documentId, principal);
    }

    @GetMapping("/api/document-analyses/{id}")
    public AnalysisDtos.AnalysisResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return analysisService.get(id, principal);
    }

    @GetMapping("/api/documents/{id}/latest-analysis")
    public AnalysisDtos.AnalysisResponse latest(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return analysisService.latest(id, principal);
    }

    @GetMapping("/api/tenders/{id}/analyses")
    public List<AnalysisDtos.AnalysisResponse> tenderAnalyses(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return analysisService.byTender(id, principal);
    }

    @PostMapping("/api/tenders/{id}/analysis/apply-field")
    public AnalysisDtos.AnalysisResponse applyField(@PathVariable Long id, @RequestBody AnalysisDtos.ApplyFieldRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return analysisService.applyField(id, request, principal);
    }

    @PostMapping("/api/tenders/{id}/analysis/apply-checklist")
    public List<SprintFourDtos.ChecklistItemResponse> applyChecklist(@PathVariable Long id, @RequestBody AnalysisDtos.ApplyChecklistRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return analysisService.applyChecklist(id, request, principal);
    }

    @GetMapping("/api/documents/{id}/search")
    public List<AnalysisDtos.DocumentSearchResult> searchDocument(@PathVariable Long id, @RequestParam String q, @AuthenticationPrincipal UserPrincipal principal) {
        return analysisService.searchDocument(id, q, principal);
    }
}
