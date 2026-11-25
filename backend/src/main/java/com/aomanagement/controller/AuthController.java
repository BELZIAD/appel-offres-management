package com.aomanagement.controller;

import com.aomanagement.dto.AuthDtos;
import com.aomanagement.security.UserPrincipal;
import com.aomanagement.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        return authService.login(request, httpRequest, response);
    }

    @PostMapping("/refresh")
    public AuthDtos.AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return authService.refresh(request, response);
    }

    @PostMapping("/logout")
    public void logout(@AuthenticationPrincipal UserPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        authService.logout(principal, request, response);
    }

    @PostMapping("/logout-all")
    public void logoutAll(@AuthenticationPrincipal UserPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        authService.logoutAll(principal, request, response);
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal);
    }
}
