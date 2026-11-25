package com.aomanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    private String ipAddress;

    @Column(length = 120)
    private String lastIpAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(nullable = false)
    private LocalDateTime loginAt;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    private LocalDateTime logoutAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean revoked = false;

    private LocalDateTime revokedAt;
}
