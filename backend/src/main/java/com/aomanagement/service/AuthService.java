package com.aomanagement.service;

import com.aomanagement.dto.AuthDtos;
import com.aomanagement.entity.User;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.repository.UserRepository;
import com.aomanagement.security.JwtService;
import com.aomanagement.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final MapperService mapperService;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService userSessionService;
    private final SecurityAuditService securityAuditService;
    private final LoginRateLimiter loginRateLimiter;

    @Value("${app.auth.max-failed-attempts:${app.security.max-failed-attempts:5}}")
    private int maxFailedAttempts;

    @Value("${app.auth.lock-minutes:${app.security.lock-minutes:15}}")
    private int lockMinutes;

    @Value("${app.refresh.expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${app.cookies.secure:false}")
    private boolean secureCookies;

    @Value("${app.cookies.same-site:Lax}")
    private String sameSite;

    @Transactional(noRollbackFor = {BadCredentialsException.class, ForbiddenOperationException.class})
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String ip = clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        loginRateLimiter.check(ip);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailure(user, email, ip, userAgent, "BAD_CREDENTIALS");
            throw new BadCredentialsException("Identifiants invalides");
        }

        unlockIfExpired(user, email, ip, userAgent);
        if (!user.isEnabled()) {
            securityAuditService.record(user, email, ip, "LOGIN_FAILURE", "ACCOUNT_DISABLED", userAgent);
            throw new ForbiddenOperationException("Compte desactive");
        }
        if (user.isAccountLocked()) {
            securityAuditService.record(user, email, ip, "ACCOUNT_LOCKED", "LOCK_ACTIVE", userAgent);
            throw new ForbiddenOperationException("Compte temporairement verrouille");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        UserPrincipal principal = new UserPrincipal(user);
        UserSessionService.CreatedSession created = userSessionService.create(user, ip, userAgent, refreshExpirationMs);
        securityAuditService.record(user, email, ip, "LOGIN_SUCCESS", "OK", userAgent);
        addRefreshCookie(response, created.refreshToken(), refreshExpirationMs);
        return new AuthDtos.AuthResponse(jwtService.generateToken(principal, created.session().getId()), mapperService.toUserResponse(user));
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        UserSessionService.CreatedSession rotated = userSessionService.rotate(refreshToken(request), clientIp(request), request.getHeader("User-Agent"));
        User user = rotated.session().getUser();
        if (!user.isEnabled()) {
            throw new ForbiddenOperationException("Compte desactive");
        }
        UserPrincipal principal = new UserPrincipal(user);
        securityAuditService.record(user, user.getEmail(), clientIp(request), "TOKEN_REFRESH", "OK", request.getHeader("User-Agent"));
        addRefreshCookie(response, rotated.refreshToken(), refreshExpirationMs);
        return new AuthDtos.AuthResponse(jwtService.generateToken(principal, rotated.session().getId()), mapperService.toUserResponse(user));
    }

    @Transactional
    public void logout(UserPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        userSessionService.logoutByRefreshToken(refreshToken(request));
        if (principal != null) {
            userRepository.findById(principal.getId()).ifPresent(user -> {
                user.setLastLogoutAt(LocalDateTime.now());
                securityAuditService.record(user, user.getEmail(), clientIp(request), "LOGOUT", "OK", request.getHeader("User-Agent"));
            });
        }
        clearRefreshCookie(response);
    }

    @Transactional
    public void logoutAll(UserPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        userSessionService.logoutAll(principal.getId());
        userRepository.findById(principal.getId()).ifPresent(user -> {
            user.setLastLogoutAt(LocalDateTime.now());
            securityAuditService.record(user, user.getEmail(), clientIp(request), "LOGOUT_ALL", "OK", request.getHeader("User-Agent"));
        });
        clearRefreshCookie(response);
    }

    public AuthDtos.UserResponse me(UserPrincipal principal) {
        User user = userRepository.findByEmailIgnoreCase(principal.getUsername()).orElseThrow();
        return mapperService.toUserResponse(user);
    }

    private void registerFailure(User user, String email, String ip, String userAgent, String reason) {
        if (user != null) {
            unlockIfExpired(user, email, ip, userAgent);
            if (!user.isAccountLocked()) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                    user.setAccountLocked(true);
                    user.setLockExpiresAt(LocalDateTime.now().plusMinutes(Math.max(1, lockMinutes)));
                    securityAuditService.record(user, email, ip, "ACCOUNT_LOCKED", "TOO_MANY_FAILURES", userAgent);
                }
            }
        }
        securityAuditService.record(user, email, ip, "LOGIN_FAILURE", reason, userAgent);
    }

    private void unlockIfExpired(User user, String email, String ip, String userAgent) {
        if (user != null && user.isAccountLocked() && user.getLockExpiresAt() != null && user.getLockExpiresAt().isBefore(LocalDateTime.now())) {
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLockExpiresAt(null);
            securityAuditService.record(user, email, ip, "ACCOUNT_UNLOCKED", "LOCK_EXPIRED", userAgent);
        }
    }

    private String refreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "ao_refresh".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken, long maxAgeMs) {
        String cookie = "ao_refresh=" + refreshToken
                + "; Path=/api/auth"
                + "; Max-Age=" + Math.max(60, maxAgeMs / 1000)
                + "; HttpOnly"
                + "; SameSite=" + sameSite
                + (secureCookies ? "; Secure" : "");
        response.addHeader("Set-Cookie", cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", "ao_refresh=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=" + sameSite + (secureCookies ? "; Secure" : ""));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
