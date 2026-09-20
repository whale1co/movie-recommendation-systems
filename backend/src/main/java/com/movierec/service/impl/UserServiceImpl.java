package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movierec.common.PageResponse;
import com.movierec.dto.UserProfileVO;
import com.movierec.dto.request.AdminUserCreateRequest;
import com.movierec.dto.request.AdminUserUpdateRequest;
import com.movierec.entity.User;
import com.movierec.exception.BusinessException;
import com.movierec.exception.ConflictException;
import com.movierec.exception.ForbiddenOperationException;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.exception.UnauthorizedException;
import com.movierec.mapper.UserAuthorizationMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.service.UserService;
import com.movierec.service.AuthSessionService;
import com.movierec.service.SecurityAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private static final String ACTIVE = "ACTIVE";
    private static final String DELETED = "DELETED";

    private final UserMapper userMapper;
    private final UserAuthorizationMapper authorizationMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService sessionService;
    private final SecurityAuditService auditService;

    public UserServiceImpl(UserMapper userMapper, UserAuthorizationMapper authorizationMapper,
                           PasswordEncoder passwordEncoder, AuthSessionService sessionService,
                           SecurityAuditService auditService) {
        this.userMapper = userMapper;
        this.authorizationMapper = authorizationMapper;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public User register(String username, String password, String preferences) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectOne(wrapper) != null) {
            throw new ConflictException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setStatus(ACTIVE);
        user.setPreferences(preferences);
        userMapper.insert(user);
        syncRole(user.getId(), "USER");
        return user;
    }

    @Override public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override public User getById(Long userId) { return userMapper.selectById(userId); }

    @Override public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : toProfileVO(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(Long userId, String oldPassword, String newPassword, String preferences) {
        User user = activeUser(userId);
        if (oldPassword != null && !oldPassword.isEmpty()) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "原密码错误");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            sessionService.revokeAllForUser(userId, "password_changed");
            auditService.record("PASSWORD_CHANGE", "SUCCESS", userId, null, null);
        }
        if (preferences != null) user.setPreferences(preferences);
        userMapper.updateById(user);
        return toProfileVO(user);
    }

    @Override
    public PageResponse<UserProfileVO> listAdminUsers(long current, long size) {
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(User::getStatus, DELETED).orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(page, wrapper);
        List<UserProfileVO> records = result.getRecords().stream().map(this::toProfileVO).toList();
        return new PageResponse<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public UserProfileVO createAdminUser(AdminUserCreateRequest request) {
        User user = register(request.username(), request.password(), request.preferences());
        String role = request.role() == null ? "USER" : request.role();
        user.setRole(role);
        user.setStatus(request.status() == null ? ACTIVE : request.status());
        userMapper.updateById(user);
        syncRole(user.getId(), role);
        return toProfileVO(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateAdminUser(Long operatorId, Long userId, AdminUserUpdateRequest request) {
        User user = requireUser(userId);
        if (operatorId.equals(userId) && ("DISABLED".equals(request.status()) || "DELETED".equals(request.status()) || "USER".equals(request.role()))) {
            throw new ForbiddenOperationException("管理员不能禁用、注销或降级自己的账户");
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
            sessionService.revokeAllForUser(userId, "admin_password_reset");
        }
        if (request.preferences() != null) user.setPreferences(request.preferences());
        if (request.role() != null) { user.setRole(request.role()); syncRole(userId, request.role()); }
        if (request.status() != null) {
            user.setStatus(request.status());
            if (!ACTIVE.equals(request.status())) sessionService.revokeAllForUser(userId, "account_status_changed");
        }
        userMapper.updateById(user);
        return toProfileVO(user);
    }

    @Override public UserProfileVO getAdminUser(Long userId) { return toProfileVO(requireUser(userId)); }

    @Override
    @Transactional
    public void deleteAdminUser(Long operatorId, Long userId) {
        if (operatorId.equals(userId)) throw new ForbiddenOperationException("不能注销当前管理员账户");
        User user = requireUser(userId);
        user.setStatus(DELETED);
        userMapper.updateById(user);
        sessionService.revokeAllForUser(userId, "account_deleted_by_admin");
    }

    @Override
    @Transactional
    public void deleteSelf(Long userId) {
        User user = activeUser(userId);
        user.setStatus(DELETED);
        userMapper.updateById(user);
        sessionService.revokeAllForUser(userId, "self_deleted");
    }

    private User activeUser(Long userId) {
        User user = requireUser(userId);
        if (!isActive(user)) throw new UnauthorizedException("账户不可用");
        return user;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new ResourceNotFoundException("用户不存在");
        return user;
    }

    private boolean isActive(User user) { return user.getStatus() == null || ACTIVE.equalsIgnoreCase(user.getStatus()); }

    private void syncRole(Long userId, String roleCode) {
        Long roleId = authorizationMapper.findRoleId(roleCode);
        if (roleId == null) throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "角色配置缺失");
        authorizationMapper.deleteUserRoles(userId);
        authorizationMapper.addUserRole(userId, roleId);
    }

    private UserProfileVO toProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole() == null ? "USER" : user.getRole());
        vo.setStatus(user.getStatus() == null ? ACTIVE : user.getStatus());
        vo.setPreferences(user.getPreferences());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
