package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.dto.request.CreateFavoriteRequest;
import com.movierec.dto.response.FavoriteResponse;
import com.movierec.entity.Favorite;
import com.movierec.entity.User;
import com.movierec.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "收藏")
@Validated
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "收藏电影")
    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateFavoriteRequest request) {
        Favorite favorite = favoriteService.addFavorite(currentUser.getId(), request.movieId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("收藏成功", FavoriteResponse.from(favorite)));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{movieId}")
    public ApiResponse<Void> removeFavorite(
            @AuthenticationPrincipal User currentUser,
            @Positive(message = "电影ID必须为正数") @PathVariable Long movieId) {
        favoriteService.removeFavorite(currentUser.getId(), movieId);
        return ApiResponse.success("取消收藏成功", null);
    }
}
