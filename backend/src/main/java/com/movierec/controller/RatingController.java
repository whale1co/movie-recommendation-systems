package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.dto.request.CreateRatingRequest;
import com.movierec.dto.request.UpdateRatingRequest;
import com.movierec.dto.response.RatingResponse;
import com.movierec.entity.Rating;
import com.movierec.entity.User;
import com.movierec.service.RatingService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评分")
@Validated
@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @Operation(summary = "创建评分")
    @PostMapping
    public ResponseEntity<ApiResponse<RatingResponse>> addRating(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateRatingRequest request) {
        Rating rating = ratingService.addRating(currentUser.getId(), request.movieId(), request.score());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("评分成功", RatingResponse.from(rating)));
    }

    @Operation(summary = "修改评分")
    @PutMapping("/{id}")
    public ApiResponse<RatingResponse> updateRating(
            @AuthenticationPrincipal User currentUser,
            @Positive(message = "评分ID必须为正数") @PathVariable Long id,
            @Valid @RequestBody UpdateRatingRequest request) {
        Rating rating = ratingService.updateRating(id, currentUser.getId(), request.score());
        return ApiResponse.success("修改成功", RatingResponse.from(rating));
    }

    @Operation(summary = "删除评分")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRating(
            @AuthenticationPrincipal User currentUser,
            @Positive(message = "评分ID必须为正数") @PathVariable Long id) {
        ratingService.deleteRating(id, currentUser.getId());
        return ApiResponse.success("删除成功", null);
    }
}
