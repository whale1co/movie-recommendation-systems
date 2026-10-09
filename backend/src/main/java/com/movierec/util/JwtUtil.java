package com.movierec.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Locale;

@Component
public class JwtUtil {

    private final Key key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms:604800000}") long expiration) {
        if (isWeakSecret(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET must contain at least 32 UTF-8 bytes and must not be a placeholder or low-entropy value");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    private boolean isWeakSecret(String secret) {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) return true;
        String normalized = secret.toLowerCase(Locale.ROOT);
        if (normalized.contains("change-me") || normalized.contains("replace-with")
                || normalized.contains("your-secret") || normalized.contains("jwt-secret")) return true;
        return secret.chars().distinct().count() < 10;
    }

    public String generateAccessToken(Long userId, String username, String sessionId) {
        return generateAccessToken(userId, username, null, sessionId);
    }

    public String generateAccessToken(Long userId, String username, String role, String sessionId) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expiration);
        var builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .claim("sessionId", sessionId)
                .setIssuedAt(now)
                .setExpiration(expire);
        if (role != null && !role.isBlank()) builder.claim("role", role);
        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public long getExpirationSeconds() {
        return expiration / 1000;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public String getSessionIdFromToken(String token) {
        return parseToken(token).get("sessionId", String.class);
    }

    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
