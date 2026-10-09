package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.dto.FavoriteVO;
import com.movierec.dto.RatingVO;
import com.movierec.dto.UserProfileVO;
import com.movierec.dto.request.UpdateProfileRequest;
import com.movierec.entity.User;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.service.FavoriteService;
import com.movierec.service.RatingService;
import com.movierec.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "当前用户")
@RestController
@RequestMapping("/api/v1/users/me")
@PreAuthorize("isAuthenticated()")
public class UserController {
    private final UserService userService;
    private final RatingService ratingService;
    private final FavoriteService favoriteService;

    public UserController(UserService userService, RatingService ratingService, FavoriteService favoriteService) {
        this.userService = userService;
        this.ratingService = ratingService;
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "查看本人资料")
    @GetMapping
    public ApiResponse<UserProfileVO> getProfile(@AuthenticationPrincipal User currentUser) {
        UserProfileVO profile = userService.getProfile(currentUser.getId());
        if (profile == null) throw new ResourceNotFoundException("用户不存在");
        return ApiResponse.success("查询成功", profile);
    }

    @Operation(summary = "修改本人资料")
    @PatchMapping
    public ApiResponse<UserProfileVO> updateProfile(@AuthenticationPrincipal User currentUser,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("修改成功", userService.updateProfile(currentUser.getId(), request.oldPassword(), request.newPassword(), request.preferences()));
    }

    @Operation(summary = "注销本人账户")
    @DeleteMapping
    public ApiResponse<Void> deleteProfile(@AuthenticationPrincipal User currentUser) {
        userService.deleteSelf(currentUser.getId());
        return ApiResponse.success("账户已注销", null);
    }

    @Operation(summary = "查看本人评分")
    @GetMapping("/ratings")
    public ApiResponse<List<RatingVO>> getUserRatings(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("查询成功", ratingService.getUserRatings(currentUser.getId()));
    }

    @Operation(summary = "查看本人收藏")
    @GetMapping("/favorites")
    public ApiResponse<List<FavoriteVO>> getUserFavorites(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("查询成功", favoriteService.getUserFavorites(currentUser.getId()));
    }
}