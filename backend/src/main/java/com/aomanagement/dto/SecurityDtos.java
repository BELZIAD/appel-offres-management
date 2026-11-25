package com.aomanagement.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public class SecurityDtos {
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
    public record ResetPasswordRequest(@NotBlank String newPassword) {}
    public record AccountStatusRequest(boolean enabled, boolean locked) {}
    public record SessionResponse(String id, String ipAddress, String userAgent, LocalDateTime loginAt, LocalDateTime lastSeenAt, LocalDateTime logoutAt, LocalDateTime expiresAt, boolean active) {}
    public record SessionListResponse(List<SessionResponse> sessions) {}
    public record SecurityAuditResponse(Long id, Long userId, String email, String ipAddress, String action, String reason, String userAgent, LocalDateTime createdAt) {}
}
