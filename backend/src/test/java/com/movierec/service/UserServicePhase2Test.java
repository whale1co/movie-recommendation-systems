package com.movierec.service;

import com.movierec.dto.request.AdminUserUpdateRequest;
import com.movierec.entity.User;
import com.movierec.exception.ForbiddenOperationException;
import com.movierec.mapper.UserAuthorizationMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServicePhase2Test {
    @Mock UserMapper userMapper;
    @Mock UserAuthorizationMapper authorizationMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthSessionService sessionService;
    @Mock SecurityAuditService auditService;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() { service = new UserServiceImpl(userMapper, authorizationMapper, passwordEncoder, sessionService, auditService); }

    @Test
    void administratorCannotDisableOwnAccount() {
        when(userMapper.selectById(7L)).thenReturn(user(7L, "ACTIVE"));
        AdminUserUpdateRequest request = new AdminUserUpdateRequest(null, null, null, "DISABLED");
        assertThrows(ForbiddenOperationException.class, () -> service.updateAdminUser(7L, 7L, request));
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void selfDeletionChangesStatusWithoutDeletingRow() {
        User user = user(7L, "ACTIVE");
        when(userMapper.selectById(7L)).thenReturn(user);
        service.deleteSelf(7L);
        org.junit.jupiter.api.Assertions.assertEquals("DELETED", user.getStatus());
        verify(userMapper).updateById(user);
        verify(sessionService).revokeAllForUser(7L, "self_deleted");
    }

    @Test
    void passwordChangeRevokesEverySession() {
        User user = user(7L, "ACTIVE");
        when(userMapper.selectById(7L)).thenReturn(user);
        when(passwordEncoder.matches("Oldpass1", "hash")).thenReturn(true);
        when(passwordEncoder.encode("Newpass2")).thenReturn("new-hash");

        service.updateProfile(7L, "Oldpass1", "Newpass2", null);

        verify(sessionService).revokeAllForUser(7L, "password_changed");
        verify(auditService).record("PASSWORD_CHANGE", "SUCCESS", 7L, null, null);
    }

    private User user(Long id, String status) {
        User user = new User();
        user.setId(id); user.setUsername("alice"); user.setPassword("hash"); user.setRole("USER"); user.setStatus(status);
        return user;
    }
}
