package com.movierec.controller;

import com.movierec.entity.Favorite;
import com.movierec.entity.User;
import com.movierec.service.FavoriteService;
import com.movierec.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public Result<Favorite> addFavorite(@AuthenticationPrincipal User currentUser,
                                        @RequestBody Map<String, Object> body) {
        Long movieId = Long.valueOf(body.get("movieId").toString());

        try {
            Favorite favorite = favoriteService.addFavorite(currentUser.getId(), movieId);
            return Result.success("收藏成功", favorite);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{movieId}")
    public Result<Void> removeFavorite(@AuthenticationPrincipal User currentUser,
                                       @PathVariable Long movieId) {
        favoriteService.removeFavorite(currentUser.getId(), movieId);
        return Result.success("取消收藏成功");
    }
}
