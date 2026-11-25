package com.aomanagement.controller;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public Page<TenderDtos.NotificationResponse> list(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return notificationService.list(principal.getId(), pageable);
    }

    @GetMapping("/unread-count")
    public TenderDtos.UnreadCountResponse unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.unreadCount(principal.getId());
    }

    @PatchMapping("/{id}/read")
    public TenderDtos.NotificationResponse read(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.markRead(id, principal.getId());
    }

    @PatchMapping("/read-all")
    public void readAll(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.getId());
    }
}
