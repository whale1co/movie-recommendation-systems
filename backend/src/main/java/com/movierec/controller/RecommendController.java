package com.movierec.controller;

import com.movierec.entity.Movie;
import com.movierec.entity.User;
import com.movierec.service.RecommendService;
import com.movierec.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/recommendations")
    public Result<List<Movie>> getRecommendations(@AuthenticationPrincipal User currentUser) {
        List<Movie> movies = recommendService.getRecommendations(currentUser.getId(), 20);
        if (movies.isEmpty()) {
            return Result.success("评分数据不足，请使用冷启动推荐", movies);
        }
        return Result.success("推荐成功", movies);
    }
}
