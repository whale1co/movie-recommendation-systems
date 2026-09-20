package com.movierec.service;

import com.movierec.entity.RefreshToken;
import com.movierec.entity.User;
import com.movierec.exception.UnauthorizedException;
import com.movierec.mapper.RefreshTokenMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {
    @Mock RefreshTokenMapper tokenMapper;
    @Mock UserMapper userMapper;
    @Mock JwtUtil jwtUtil;
    @Mock SecurityAuditService auditService;
    private AuthSessionService service;

    @BeforeEach
    void setUp() {
        service = new AuthSessionService(tokenMapper, userMapper, jwtUtil, auditService, 604800);
        org.mockito.Mockito.lenient().when(jwtUtil.getExpirationSeconds()).thenReturn(900L);
    }

    @Test
    void storesOnlyRefreshTokenHash() {
        User user = activeUser();
        when(jwtUtil.generateAccessToken(eq(7L), eq("alice"), any())).thenReturn("access");

        AuthSessionService.AuthTokens tokens = service.create(user);

        org.mockito.ArgumentCaptor<RefreshToken> captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokenMapper).insert(captor.capture());
        assertNotEquals(tokens.refreshToken(), captor.getValue().getTokenHash());
        org.junit.jupiter.api.Assertions.assertEquals(64, captor.getValue().getTokenHash().length());
    }

    @Test
    void refreshRevokesOldTokenAndIssuesNewSession() {
        RefreshToken current = new RefreshToken();
        current.setId(11L);
        current.setUserId(7L);
        current.setSessionId("old-session");
        current.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(tokenMapper.selectOne(any())).thenReturn(current);
        when(tokenMapper.revokeForRotation(eq(11L), any())).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(activeUser());
        when(jwtUtil.generateAccessToken(eq(7L), eq("alice"), any())).thenReturn("new-access");

        AuthSessionService.AuthTokens tokens = service.rotate("old-refresh");

        org.junit.jupiter.api.Assertions.assertEquals("new-access", tokens.accessToken());
        verify(tokenMapper).revokeForRotation(eq(11L), any());
        verify(tokenMapper).updateById(current);
        verify(auditService).record("TOKEN_REFRESH", "SUCCESS", 7L, null, null);
    }

    @Test
    void reusedRefreshTokenRevokesAllUserSessions() {
        RefreshToken current = new RefreshToken();
        current.setId(11L);
        current.setUserId(7L);
        current.setSessionId("old-session");
        current.setExpiresAt(LocalDateTime.now().plusDays(1));
        current.setRevokedAt(LocalDateTime.now());
        when(tokenMapper.selectOne(any())).thenReturn(current);

        assertThrows(UnauthorizedException.class, () -> service.rotate("reused-refresh"));

        verify(tokenMapper).revokeAllForUser(eq(7L), any());
    }

    private User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setStatus("ACTIVE");
        return user;
    }
}
