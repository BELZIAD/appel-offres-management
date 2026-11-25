package com.aomanagement.dto;

import com.aomanagement.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UserDtos {
    public record EmployeeRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String email,
            @Size(min = 8) String password,
            Boolean enabled
    ) {}

    public record EmployeeResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            Role role,
            boolean enabled,
            boolean accountLocked,
            LocalDateTime lastLoginAt,
            String lastLoginIp,
            LocalDateTime lastLogoutAt,
            long assignedTenderCount
    ) {}
}
