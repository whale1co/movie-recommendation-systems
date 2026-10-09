package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.entity.Movie;
import com.movierec.entity.User;
import com.movierec.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "推荐")
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendController {
    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @Operation(summary = "获取个性化推荐")
    @GetMapping
    public ApiResponse<List<Movie>> getRecommendations(@AuthenticationPrincipal User currentUser) {
        List<Movie> movies = recommendService.getRecommendations(currentUser.getId(), 20);
        String message = movies.isEmpty() ? "评分数据不足，请使用冷启动推荐" : "推荐成功";
        return ApiResponse.success(message, movies);
    }
}
