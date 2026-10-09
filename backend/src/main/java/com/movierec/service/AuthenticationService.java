package com.movierec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.entity.User;
import com.movierec.exception.TooManyRequestsException;
import com.movierec.exception.UnauthorizedException;
import com.movierec.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthSessionService sessionService;
    private final SecurityAuditService auditService;

    public AuthenticationService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                                 LoginAttemptService loginAttemptService, AuthSessionService sessionService,
                                 SecurityAuditService auditService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    public AuthSessionService.AuthTokens login(String username, String password, String ipAddress) {
        try {
            loginAttemptService.checkAllowed(username, ipAddress);
        } catch (TooManyRequestsException ex) {
            auditService.record("LOGIN_RATE_LIMIT", "DENIED", null, username, null);
            throw ex;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            loginAttemptService.recordFailure(username, ipAddress);
            auditService.record("LOGIN", "FAILURE", user == null ? null : user.getId(), username,
                    "reason=bad_credentials");
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (!(user.getStatus() == null || "ACTIVE".equalsIgnoreCase(user.getStatus()))) {
            auditService.record("LOGIN", "DENIED", user.getId(), username, "reason=inactive_account");
            throw new UnauthorizedException("账户已被禁用或注销");
        }
        loginAttemptService.recordSuccess(username, ipAddress);
        AuthSessionService.AuthTokens tokens = sessionService.create(user);
        auditService.record("LOGIN", "SUCCESS", user.getId(), username, null);
        return tokens;
    }

    public AuthSessionService.AuthTokens refresh(String refreshToken) {
        return sessionService.rotate(refreshToken);
    }
}
