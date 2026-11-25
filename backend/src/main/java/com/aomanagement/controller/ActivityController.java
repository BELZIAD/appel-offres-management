package com.aomanagement.controller;

import com.aomanagement.dto.SprintFourDtos;
import com.aomanagement.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityLogService activityLogService;

    @GetMapping
    public Page<SprintFourDtos.ActivityResponse> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long tenderId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            Pageable pageable
    ) {
        Pageable effective = pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return activityLogService.search(userId, tenderId, action, dateFrom, dateTo, effective);
    }
}
