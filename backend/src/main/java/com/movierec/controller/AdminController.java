package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.service.CrawlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "管理")
@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final CrawlService crawlService;
    private final MovieMapper movieMapper;
    private final UserMapper userMapper;
    private final RatingMapper ratingMapper;

    public AdminController(CrawlService crawlService, MovieMapper movieMapper,
                           UserMapper userMapper, RatingMapper ratingMapper) {
        this.crawlService = crawlService;
        this.movieMapper = movieMapper;
        this.userMapper = userMapper;
        this.ratingMapper = ratingMapper;
    }

    @Operation(summary = "查看系统统计")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "OK");
        status.put("service", "movie-rec-backend");
        status.put("movieCount", movieMapper.selectCount(null));
        status.put("userCount", userMapper.selectCount(null));
        status.put("ratingCount", ratingMapper.selectCount(null));
        return ApiResponse.success("系统运行正常", status);
    }

    @Operation(summary = "爬取 Top 250")
    @PostMapping("/crawl")
    public ApiResponse<Map<String, Integer>> startCrawl() {
        return ApiResponse.success("爬取完成", crawlService.crawlTop250());
    }

    @Operation(summary = "按页爬取电影")
    @PostMapping("/crawl-movies")
    public ApiResponse<Map<String, Integer>> crawlMovies(
            @Min(value = 1, message = "页数不能小于1")
            @Max(value = 500, message = "页数不能超过500")
            @RequestParam(defaultValue = "250") int pages) {
        return ApiResponse.success("爬取完成", crawlService.crawlMovies(pages));
    }

    @Operation(summary = "下载海报")
    @PostMapping("/fetch-posters")
    public ApiResponse<Map<String, Integer>> fetchPosters() {
        return ApiResponse.success("海报下载完成", crawlService.fetchPosters());
    }

    @Operation(summary = "导入 CSV")
    @PostMapping("/import-csv")
    public ApiResponse<Map<String, Integer>> importCsv() {
        return ApiResponse.success("CSV导入完成", crawlService.importFromCsv());
    }
}
