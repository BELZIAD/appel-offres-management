package com.aomanagement.dto;

import com.aomanagement.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class AuthDtos {
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, UserResponse user) {}
    public record UserResponse(Long id, String firstName, String lastName, String email, Role role, boolean enabled, boolean accountLocked, LocalDateTime lastLoginAt, String lastLoginIp, LocalDateTime lastLogoutAt) {}
}
