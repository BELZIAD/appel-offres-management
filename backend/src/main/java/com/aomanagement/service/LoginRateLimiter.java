package com.aomanagement.service;

import com.aomanagement.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowSeconds;

    public LoginRateLimiter(
            @Value("${app.auth.login-rate-limit:10}") int limit,
            @Value("${app.auth.login-rate-window-seconds:60}") long windowSeconds
    ) {
        this.limit = Math.max(1, limit);
        this.windowSeconds = Math.max(1, windowSeconds);
    }

    public void check(String ip) {
        long now = Instant.now().getEpochSecond();
        Window window = windows.compute(ip == null ? "unknown" : ip, (key, current) -> {
            if (current == null || now >= current.resetAt) {
                return new Window(1, now + windowSeconds);
            }
            current.count++;
            return current;
        });
        if (window.count > limit) {
            throw new TooManyRequestsException("Trop de tentatives. Veuillez patienter avant de reessayer.", Math.max(1, window.resetAt - now));
        }
    }

    private static class Window {
        private int count;
        private final long resetAt;

        private Window(int count, long resetAt) {
            this.count = count;
            this.resetAt = resetAt;
        }
    }
}
