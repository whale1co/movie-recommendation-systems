package com.movierec.controller;

import com.movierec.dto.FavoriteVO;
import com.movierec.dto.RatingVO;
import com.movierec.dto.UserProfileVO;
import com.movierec.entity.User;
import com.movierec.service.FavoriteService;
import com.movierec.service.RatingService;
import com.movierec.service.UserService;
import com.movierec.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final RatingService ratingService;
    private final FavoriteService favoriteService;

    public UserController(UserService userService, RatingService ratingService, FavoriteService favoriteService) {
        this.userService = userService;
        this.ratingService = ratingService;
        this.favoriteService = favoriteService;
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String preferences = body.get("preferences");

        try {
            User user = userService.register(username, password, preferences);
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            return Result.success("注册成功", data);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        try {
            String token = userService.login(username, password);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            return Result.success("登录成功", data);
        } catch (RuntimeException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @GetMapping("/me")
    public Result<UserProfileVO> getProfile(@AuthenticationPrincipal User currentUser) {
        UserProfileVO profile = userService.getProfile(currentUser.getId());
        if (profile == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success("查询成功", profile);
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(@AuthenticationPrincipal User currentUser,
                                               @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String preferences = body.get("preferences");

        try {
            UserProfileVO profile = userService.updateProfile(currentUser.getId(), oldPassword, newPassword, preferences);
            return Result.success("修改成功", profile);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/ratings")
    public Result<List<RatingVO>> getUserRatings(@AuthenticationPrincipal User currentUser) {
        List<RatingVO> ratings = ratingService.getUserRatings(currentUser.getId());
        return Result.success("查询成功", ratings);
    }

    @GetMapping("/favorites")
    public Result<List<FavoriteVO>> getUserFavorites(@AuthenticationPrincipal User currentUser) {
        List<FavoriteVO> favorites = favoriteService.getUserFavorites(currentUser.getId());
        return Result.success("查询成功", favorites);
    }
}
