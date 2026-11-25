package com.aomanagement.service;

import com.aomanagement.dto.SecurityDtos;
import com.aomanagement.entity.User;
import com.aomanagement.entity.UserSession;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSessionService {
    private final UserSessionRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public CreatedSession create(User user, String ip, String userAgent, long expirationMs) {
        String refreshToken = randomToken(48);
        LocalDateTime now = LocalDateTime.now();
        UserSession session = new UserSession();
        session.setId(randomToken(32));
        session.setUser(user);
        session.setIpAddress(ip);
        session.setLastIpAddress(ip);
        session.setUserAgent(trim(userAgent));
        session.setRefreshTokenHash(hash(refreshToken));
        session.setLoginAt(now);
        session.setLastSeenAt(now);
        session.setExpiresAt(now.plusSeconds(Math.max(60, expirationMs / 1000)));
        return new CreatedSession(repository.save(session), refreshToken);
    }

    @Transactional
    public boolean touch(String id) {
        return repository.findById(id)
                .filter(this::usable)
                .map(session -> {
                    session.setLastSeenAt(LocalDateTime.now());
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public CreatedSession rotate(String refreshToken, String ip, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new com.aomanagement.exception.UnauthorizedException("Session absente");
        }
        UserSession session = repository.findByRefreshTokenHash(hash(refreshToken))
                .orElseThrow(() -> new com.aomanagement.exception.UnauthorizedException("Session invalide"));
        if (!usable(session)) {
            session.setActive(false);
            session.setRevoked(true);
            session.setRevokedAt(LocalDateTime.now());
            throw new com.aomanagement.exception.UnauthorizedException("Session expiree ou revoquee");
        }
        String newRefreshToken = randomToken(48);
        session.setRefreshTokenHash(hash(newRefreshToken));
        session.setLastIpAddress(ip);
        session.setUserAgent(trim(userAgent));
        session.setLastSeenAt(LocalDateTime.now());
        return new CreatedSession(session, newRefreshToken);
    }

    @Transactional
    public void logout(String id) {
        repository.findById(id).ifPresent(session -> {
            session.setActive(false);
            session.setRevoked(true);
            session.setRevokedAt(LocalDateTime.now());
            session.setLogoutAt(LocalDateTime.now());
        });
    }

    @Transactional
    public void logout(String id, Long ownerId) {
        repository.findById(id)
                .filter(session -> session.getUser().getId().equals(ownerId))
                .ifPresent(session -> {
                    session.setActive(false);
                    session.setRevoked(true);
                    session.setRevokedAt(LocalDateTime.now());
                    session.setLogoutAt(LocalDateTime.now());
                });
    }

    @Transactional
    public void logoutByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        repository.findByRefreshTokenHash(hash(refreshToken)).ifPresent(session -> {
            session.setActive(false);
            session.setRevoked(true);
            session.setRevokedAt(LocalDateTime.now());
            session.setLogoutAt(LocalDateTime.now());
        });
    }

    @Transactional
    public void logoutAll(Long userId) {
        repository.findByUserIdAndActiveTrueOrderByLoginAtDesc(userId).forEach(session -> {
            session.setActive(false);
            session.setRevoked(true);
            session.setRevokedAt(LocalDateTime.now());
            session.setLogoutAt(LocalDateTime.now());
        });
    }

    public SecurityDtos.SessionListResponse sessions(Long userId) {
        List<SecurityDtos.SessionResponse> sessions = repository.findByUserIdOrderByLoginAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
        return new SecurityDtos.SessionListResponse(sessions);
    }

    public long activeCount() {
        return repository.countByActiveTrueAndExpiresAtAfter(LocalDateTime.now());
    }

    private SecurityDtos.SessionResponse toResponse(UserSession session) {
        return new SecurityDtos.SessionResponse(session.getId(), session.getIpAddress(), session.getUserAgent(), session.getLoginAt(), session.getLastSeenAt(), session.getLogoutAt(), session.getExpiresAt(), session.isActive());
    }

    private boolean usable(UserSession session) {
        return session.isActive() && !session.isRevoked() && session.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private String randomToken(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hash(String token) {
        if (token == null) return "";
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Hash refresh token impossible", ex);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.substring(0, Math.min(value.length(), 500));
    }

    public record CreatedSession(UserSession session, String refreshToken) {}
}
