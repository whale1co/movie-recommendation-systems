package com.movierec.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.movierec.dto.MovieDetailVO;
import com.movierec.dto.MovieSearchVO;
import com.movierec.entity.Movie;
import com.movierec.entity.User;
import com.movierec.service.MovieService;
import com.movierec.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        IPage<MovieSearchVO> result = movieService.search(q, page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());

        return Result.success("查询成功", data);
    }

    @GetMapping("/{id}")
    public Result<MovieDetailVO> getDetail(@PathVariable Long id) {
        MovieDetailVO detail = movieService.getDetail(id);
        if (detail == null) {
            return Result.error(404, "电影不存在");
        }
        return Result.success("查询成功", detail);
    }

    @GetMapping("/hot")
    public Result<List<Movie>> getHotMovies(@RequestParam(defaultValue = "20") int limit) {
        List<Movie> movies = movieService.getHotMovies(limit);
        return Result.success("查询成功", movies);
    }

    @GetMapping("/cold-start")
    public Result<List<Movie>> getColdStartRecommendations(@AuthenticationPrincipal User currentUser) {
        try {
            List<Movie> movies = movieService.getColdStartMovies(currentUser.getId());
            return Result.success("冷启动推荐成功", movies);
        } catch (RuntimeException e) {
            return Result.error(200, e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String genre) {

        IPage<MovieSearchVO> result = movieService.listByGenre(page, size, genre);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());

        return Result.success("查询成功", data);
    }
}
