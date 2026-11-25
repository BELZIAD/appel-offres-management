package com.aomanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserPrincipal principal) {
        return generateToken(principal, null);
    }

    public String generateToken(UserPrincipal principal, String sessionId) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(principal.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .claim("uid", principal.getId())
                .id(UUID.randomUUID().toString())
                .issuer("ao-management")
                .signWith(key);
        if (sessionId != null) {
            builder.claim("sid", sessionId);
        }
        return builder.compact();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public boolean isValid(String token, UserPrincipal principal) {
        return principal.getUsername().equals(extractUsername(token)) && claims(token).getExpiration().after(new Date());
    }

    public String extractSessionId(String token) {
        Object sid = claims(token).get("sid");
        return sid == null ? null : sid.toString();
    }

    public long expirationMs() {
        return expirationMs;
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
