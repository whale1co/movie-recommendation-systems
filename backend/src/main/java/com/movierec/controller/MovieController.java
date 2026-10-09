package com.movierec.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.movierec.common.ApiResponse;
import com.movierec.common.PageResponse;
import com.movierec.dto.MovieDetailVO;
import com.movierec.dto.MovieSearchVO;
import com.movierec.entity.Movie;
import com.movierec.entity.User;
import com.movierec.exception.ResourceNotFoundException;
import com.movierec.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "电影")
@Validated
@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @Operation(summary = "搜索电影")
    @GetMapping("/search")
    public ApiResponse<PageResponse<MovieSearchVO>> search(
            @Size(max = 100, message = "搜索词不能超过100个字符")
            @RequestParam(defaultValue = "") String q,
            @Min(value = 1, message = "页码不能小于1")
            @RequestParam(defaultValue = "1") int page,
            @Min(value = 1, message = "每页数量不能小于1")
            @Max(value = 100, message = "每页数量不能超过100")
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("查询成功", toPage(movieService.search(q.trim(), page, size)));
    }

    @Operation(summary = "查看电影详情")
    @GetMapping("/{id}")
    public ApiResponse<MovieDetailVO> getDetail(
            @Positive(message = "电影ID必须为正数") @PathVariable Long id) {
        MovieDetailVO detail = movieService.getDetail(id);
        if (detail == null) {
            throw new ResourceNotFoundException("电影不存在");
        }
        return ApiResponse.success("查询成功", detail);
    }

    @Operation(summary = "查看热门电影")
    @GetMapping("/hot")
    public ApiResponse<List<Movie>> getHotMovies(
            @Min(value = 1, message = "数量不能小于1")
            @Max(value = 100, message = "数量不能超过100")
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success("查询成功", movieService.getHotMovies(limit));
    }

    @Operation(summary = "获取冷启动推荐")
    @GetMapping("/cold-start")
    public ApiResponse<List<Movie>> getColdStartRecommendations(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("冷启动推荐成功",
                movieService.getColdStartMovies(currentUser.getId()));
    }

    @Operation(summary = "分页浏览电影")
    @GetMapping
    public ApiResponse<PageResponse<MovieSearchVO>> list(
            @Min(value = 1, message = "页码不能小于1")
            @RequestParam(defaultValue = "1") int page,
            @Min(value = 1, message = "每页数量不能小于1")
            @Max(value = 100, message = "每页数量不能超过100")
            @RequestParam(defaultValue = "20") int size,
            @Size(max = 50, message = "类型不能超过50个字符")
            @RequestParam(required = false) String genre) {
        return ApiResponse.success("查询成功", toPage(movieService.listByGenre(page, size, genre)));
    }

    private PageResponse<MovieSearchVO> toPage(IPage<MovieSearchVO> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
