package com.aomanagement.repository;

import com.aomanagement.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    List<UserSession> findByUserIdOrderByLoginAtDesc(Long userId);
    List<UserSession> findByUserIdAndActiveTrueOrderByLoginAtDesc(Long userId);
    long countByActiveTrueAndExpiresAtAfter(LocalDateTime now);
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
}
