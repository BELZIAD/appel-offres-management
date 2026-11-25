package com.aomanagement.controller;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.EmailReminderStatus;
import com.aomanagement.entity.TenderHistoryAction;
import com.aomanagement.service.TenderHistoryService;
import com.aomanagement.service.TenderReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminTrackingController {
    private final TenderReminderService reminderService;
    private final TenderHistoryService historyService;

    @GetMapping("/email-reminders")
    public Page<TenderDtos.EmailReminderResponse> reminders(
            @RequestParam(required = false) EmailReminderStatus status,
            @RequestParam(required = false) Long tender,
            @RequestParam(required = false) Long employee,
            Pageable pageable
    ) {
        return reminderService.search(status, tender, employee, pageable);
    }

    @GetMapping("/history")
    public Page<TenderDtos.HistoryResponse> history(
            @RequestParam(required = false) TenderHistoryAction action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tenderId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return historyService.search(action, userId, tenderId, dateFrom, dateTo, search, effectivePageable);
    }
}
