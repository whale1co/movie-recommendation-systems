package com.movierec.service;

import com.movierec.entity.User;
import com.movierec.exception.UnauthorizedException;
import com.movierec.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock LoginAttemptService loginAttemptService;
    @Mock AuthSessionService sessionService;
    @Mock SecurityAuditService auditService;

    @Test
    void disabledUserCannotCreateSession() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setPassword("hash");
        user.setStatus("DISABLED");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("Password1", "hash")).thenReturn(true);
        AuthenticationService service = new AuthenticationService(userMapper, passwordEncoder,
                loginAttemptService, sessionService, auditService);

        assertThrows(UnauthorizedException.class,
                () -> service.login("alice", "Password1", "127.0.0.1"));

        verify(auditService).record("LOGIN", "DENIED", 7L, "alice", "reason=inactive_account");
        verifyNoInteractions(sessionService);
    }
}
