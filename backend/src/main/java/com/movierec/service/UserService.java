package com.movierec.service;

import com.movierec.common.PageResponse;
import com.movierec.dto.UserProfileVO;
import com.movierec.dto.request.AdminUserCreateRequest;
import com.movierec.dto.request.AdminUserUpdateRequest;
import com.movierec.entity.User;

public interface UserService {
    User register(String username, String password, String preferences);
    User getByUsername(String username);
    User getById(Long userId);
    UserProfileVO getProfile(Long userId);
    UserProfileVO updateProfile(Long userId, String oldPassword, String newPassword, String preferences);
    PageResponse<UserProfileVO> listAdminUsers(long current, long size);
    PageResponse<UserProfileVO> listAdminUsers(long current, long size, String keyword, String status, String role);
    UserProfileVO createAdminUser(AdminUserCreateRequest request);
    UserProfileVO updateAdminUser(Long operatorId, Long userId, AdminUserUpdateRequest request);
    UserProfileVO getAdminUser(Long userId);
    void deleteAdminUser(Long operatorId, Long userId);
    void deleteSelf(Long userId);
}
