package com.aomanagement.service;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.entity.*;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.ChecklistTemplateItemRepository;
import com.aomanagement.repository.ChecklistTemplateRepository;
import com.aomanagement.repository.TenderChecklistItemRepository;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {
    private final ChecklistTemplateRepository templateRepository;
    private final ChecklistTemplateItemRepository templateItemRepository;
    private final TenderChecklistItemRepository tenderChecklistItemRepository;
    private final TenderService tenderService;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<SprintFourDtos.ChecklistTemplateResponse> templates() {
        return templateRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toTemplateResponse).toList();
    }

    @Transactional
    public SprintFourDtos.ChecklistTemplateResponse createTemplate(SprintFourDtos.ChecklistTemplateRequest request) {
        ChecklistTemplate template = new ChecklistTemplate();
        applyTemplate(template, request);
        ChecklistTemplate saved = templateRepository.save(template);
        saveTemplateItems(saved, request.items());
        return toTemplateResponse(saved);
    }

    @Transactional
    public SprintFourDtos.ChecklistTemplateResponse updateTemplate(Long id, SprintFourDtos.ChecklistTemplateRequest request) {
        ChecklistTemplate template = templateRepository.findById(id).orElseThrow(() -> new NotFoundException("Template checklist introuvable"));
        applyTemplate(template, request);
        templateItemRepository.deleteByTemplateId(id);
        saveTemplateItems(template, request.items());
        return toTemplateResponse(template);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        ChecklistTemplate template = templateRepository.findById(id).orElseThrow(() -> new NotFoundException("Template checklist introuvable"));
        templateRepository.delete(template);
    }

    @Transactional(readOnly = true)
    public List<SprintFourDtos.ChecklistItemResponse> tenderChecklist(Long tenderId, UserPrincipal principal) {
        tenderService.findSecuredEntity(tenderId, principal);
        return tenderChecklistItemRepository.findByTenderIdOrderByPositionAsc(tenderId).stream().map(mapperService::toChecklistItemResponse).toList();
    }

    @Transactional
    public List<SprintFourDtos.ChecklistItemResponse> createFromTemplate(Long tenderId, Long templateId, UserPrincipal principal) {
        Tender tender = tenderService.find(tenderId);
        User actor = currentUser(principal);
        ChecklistTemplate template = templateRepository.findById(templateId).orElseThrow(() -> new NotFoundException("Template checklist introuvable"));
        List<ChecklistTemplateItem> templateItems = templateItemRepository.findByTemplateIdOrderByPositionAsc(template.getId());
        for (ChecklistTemplateItem source : templateItems) {
            TenderChecklistItem item = new TenderChecklistItem();
            item.setTender(tender);
            item.setLabel(source.getLabel());
            item.setRequired(source.isRequired());
            item.setPosition(source.getPosition());
            item.setExpectedDocumentType(source.getExpectedDocumentType());
            tenderChecklistItemRepository.save(item);
        }
        activityLogService.record(actor, tender, "CHECKLIST", tender.getId(), "CHECKLIST_CREATED", actor.getFirstName() + " a cree une checklist pour " + tender.getReference() + ".");
        return tenderChecklistItemRepository.findByTenderIdOrderByPositionAsc(tenderId).stream().map(mapperService::toChecklistItemResponse).toList();
    }

    @Transactional
    public SprintFourDtos.ChecklistItemResponse updateItem(Long tenderId, Long itemId, boolean completed, UserPrincipal principal) {
        Tender tender = tenderService.findSecuredEntity(tenderId, principal);
        TenderChecklistItem item = tenderChecklistItemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Element checklist introuvable"));
        if (!item.getTender().getId().equals(tender.getId())) {
            throw new NotFoundException("Element checklist introuvable");
        }
        User actor = currentUser(principal);
        item.setCompleted(completed);
        item.setCompletedBy(completed ? actor : null);
        item.setCompletedAt(completed ? LocalDateTime.now() : null);
        activityLogService.record(actor, tender, "CHECKLIST_ITEM", item.getId(), "CHECKLIST_ITEM_UPDATED", actor.getFirstName() + " a mis a jour \"" + item.getLabel() + "\".");
        return mapperService.toChecklistItemResponse(item);
    }

    private void applyTemplate(ChecklistTemplate template, SprintFourDtos.ChecklistTemplateRequest request) {
        template.setName(request.name());
        template.setDescription(request.description());
        template.setActive(request.active());
    }

    private void saveTemplateItems(ChecklistTemplate template, List<SprintFourDtos.ChecklistTemplateItemRequest> items) {
        if (items == null) return;
        List<SprintFourDtos.ChecklistTemplateItemRequest> sorted = items.stream()
                .sorted(Comparator.comparingInt(SprintFourDtos.ChecklistTemplateItemRequest::position))
                .toList();
        int fallbackPosition = 1;
        for (SprintFourDtos.ChecklistTemplateItemRequest request : sorted) {
            ChecklistTemplateItem item = new ChecklistTemplateItem();
            item.setTemplate(template);
            item.setLabel(request.label());
            item.setPosition(request.position() <= 0 ? fallbackPosition : request.position());
            item.setRequired(request.required());
            item.setExpectedDocumentType(request.expectedDocumentType());
            templateItemRepository.save(item);
            fallbackPosition++;
        }
    }

    private SprintFourDtos.ChecklistTemplateResponse toTemplateResponse(ChecklistTemplate template) {
        return new SprintFourDtos.ChecklistTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.isActive(),
                template.getCreatedAt(),
                templateItemRepository.findByTemplateIdOrderByPositionAsc(template.getId()).stream().map(mapperService::toChecklistTemplateItemResponse).toList()
        );
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }
}
