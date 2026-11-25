package com.aomanagement.controller;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.ChecklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChecklistController {
    private final ChecklistService checklistService;

    @GetMapping("/api/admin/checklist-templates")
    public List<SprintFourDtos.ChecklistTemplateResponse> templates() {
        return checklistService.templates();
    }

    @PostMapping("/api/admin/checklist-templates")
    public SprintFourDtos.ChecklistTemplateResponse createTemplate(@Valid @RequestBody SprintFourDtos.ChecklistTemplateRequest request) {
        return checklistService.createTemplate(request);
    }

    @PutMapping("/api/admin/checklist-templates/{id}")
    public SprintFourDtos.ChecklistTemplateResponse updateTemplate(@PathVariable Long id, @Valid @RequestBody SprintFourDtos.ChecklistTemplateRequest request) {
        return checklistService.updateTemplate(id, request);
    }

    @DeleteMapping("/api/admin/checklist-templates/{id}")
    public void deleteTemplate(@PathVariable Long id) {
        checklistService.deleteTemplate(id);
    }

    @GetMapping("/api/tenders/{id}/checklist")
    public List<SprintFourDtos.ChecklistItemResponse> tenderChecklist(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return checklistService.tenderChecklist(id, principal);
    }

    @PostMapping("/api/admin/tenders/{id}/checklist/from-template/{templateId}")
    public List<SprintFourDtos.ChecklistItemResponse> createFromTemplate(@PathVariable Long id, @PathVariable Long templateId, @AuthenticationPrincipal UserPrincipal principal) {
        return checklistService.createFromTemplate(id, templateId, principal);
    }

    @PatchMapping("/api/tenders/{tenderId}/checklist/{itemId}")
    public SprintFourDtos.ChecklistItemResponse updateItem(@PathVariable Long tenderId, @PathVariable Long itemId, @RequestBody SprintFourDtos.ChecklistStatusRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return checklistService.updateItem(tenderId, itemId, request.completed(), principal);
    }
}
