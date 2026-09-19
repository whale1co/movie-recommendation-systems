package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.dto.UserProfileVO;
import com.movierec.entity.User;
import com.movierec.exception.BusinessException;
import com.movierec.exception.ConflictException;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.exception.UnauthorizedException;
import com.movierec.mapper.UserMapper;
import com.movierec.service.UserService;
import com.movierec.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
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
        user.setPreferences(preferences);
        userMapper.insert(user);
        return user;
    }

    @Override
    public String login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return toProfileVO(user);
    }

    @Override
    public UserProfileVO updateProfile(Long userId, String oldPassword, String newPassword, String preferences) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        if (oldPassword != null && !oldPassword.isEmpty()) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "原密码错误");
            }
            if (newPassword == null || newPassword.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "新密码不能为空");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (preferences != null) {
            user.setPreferences(preferences);
        }

        userMapper.updateById(user);
        return toProfileVO(user);
    }

    private UserProfileVO toProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole() == null ? "USER" : user.getRole());
        vo.setPreferences(user.getPreferences());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
