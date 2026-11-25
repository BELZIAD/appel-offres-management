package com.aomanagement.controller;

import com.aomanagement.dto.SecurityDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.UserService;
import com.aomanagement.service.UserSessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class SecurityController {
    private final UserSessionService userSessionService;
    private final UserService userService;

    @GetMapping("/sessions")
    public SecurityDtos.SessionListResponse sessions(@AuthenticationPrincipal UserPrincipal principal) {
        return userSessionService.sessions(principal.getId());
    }

    @DeleteMapping("/sessions/{id}")
    public void revoke(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        userSessionService.logout(id, principal.getId());
    }

    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody SecurityDtos.ChangePasswordRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        userService.changeOwnPassword(principal.getId(), request.currentPassword(), request.newPassword());
    }
}
