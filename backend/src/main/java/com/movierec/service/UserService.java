package com.movierec.service;

import com.movierec.dto.UserProfileVO;
import com.movierec.entity.User;

public interface UserService {

    User register(String username, String password, String preferences);

    String login(String username, String password);

    User getByUsername(String username);

    User getById(Long userId);

    UserProfileVO getProfile(Long userId);

    UserProfileVO updateProfile(Long userId, String oldPassword, String newPassword, String preferences);
}
