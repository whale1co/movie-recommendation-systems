package com.movierec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.entity.RefreshToken;
import com.movierec.entity.User;
import com.movierec.exception.UnauthorizedException;
import com.movierec.mapper.RefreshTokenMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthSessionService {
    private final RefreshTokenMapper tokenMapper;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final SecurityAuditService auditService;
    private final long refreshExpirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthSessionService(RefreshTokenMapper tokenMapper, UserMapper userMapper, JwtUtil jwtUtil,
                              SecurityAuditService auditService,
                              @Value("${jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds) {
        this.tokenMapper = tokenMapper;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Transactional
    public AuthTokens create(User user) {
        return persist(user, null);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthTokens rotate(String rawToken) {
        RefreshToken current = find(rawToken);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (current == null || current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)
                || tokenMapper.revokeForRotation(current.getId(), now) != 1) {
            if (current != null) revokeAllForUser(current.getUserId(), "refresh_token_reuse");
            auditService.record("TOKEN_REFRESH", "DENIED", current == null ? null : current.getUserId(),
                    null, "reason=invalid_or_reused");
            throw new UnauthorizedException("刷新令牌无效或已失效");
        }
        User user = userMapper.selectById(current.getUserId());
        if (user == null || !(user.getStatus() == null || "ACTIVE".equalsIgnoreCase(user.getStatus()))) {
            revokeAllForUser(current.getUserId(), "inactive_user");
            throw new UnauthorizedException("账户不可用");
        }
        AuthTokens next = persist(user, current);
        auditService.record("TOKEN_REFRESH", "SUCCESS", user.getId(), null, null);
        return next;
    }

    public boolean isSessionActive(String sessionId) {
        return sessionId != null
                && tokenMapper.countActiveSession(sessionId, LocalDateTime.now(ZoneOffset.UTC)) > 0;
    }

    public void revokeSession(String sessionId, Long userId, String reason) {
        if (sessionId != null) tokenMapper.revokeSession(sessionId, LocalDateTime.now(ZoneOffset.UTC));
        auditService.record("LOGOUT", "SUCCESS", userId, null, "reason=" + reason);
    }

    public void revokeByRefreshToken(String rawToken, String reason) {
        RefreshToken token = find(rawToken);
        if (token != null) revokeSession(token.getSessionId(), token.getUserId(), reason);
    }

    public void revokeAllForUser(Long userId, String reason) {
        tokenMapper.revokeAllForUser(userId, LocalDateTime.now(ZoneOffset.UTC));
        auditService.record("SESSION_REVOKE_ALL", "SUCCESS", userId, null, "reason=" + reason);
    }

    private AuthTokens persist(User user, RefreshToken replaced) {
        String rawToken = randomToken();
        String sessionId = UUID.randomUUID().toString();
        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setSessionId(sessionId);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(refreshExpirationSeconds));
        tokenMapper.insert(entity);
        if (replaced != null) {
            replaced.setReplacedByTokenId(entity.getId());
            tokenMapper.updateById(replaced);
        }
        return new AuthTokens(jwtUtil.generateAccessToken(user.getId(), user.getUsername(), sessionId),
                rawToken, jwtUtil.getExpirationSeconds());
    }

    private RefreshToken find(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        return tokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, hash(rawToken)));
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public record AuthTokens(String accessToken, String refreshToken, long accessExpiresIn) {
    }
}
