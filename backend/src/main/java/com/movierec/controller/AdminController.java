package com.movierec.controller;

import com.movierec.service.CrawlService;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/admin")
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

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "OK");
        status.put("service", "movie-rec-backend");
        status.put("timestamp", System.currentTimeMillis());
        status.put("movieCount", movieMapper.selectCount(null));
        status.put("userCount", userMapper.selectCount(null));
        status.put("ratingCount", ratingMapper.selectCount(null));
        return Result.success("系统运行正常", status);
    }

    @PostMapping("/crawl")
    public Result<Map<String, Integer>> startCrawl() {
        try {
            Map<String, Integer> stats = crawlService.crawlTop250();
            return Result.success("爬取完成", stats);
        } catch (Exception e) {
            return Result.error(500, "爬取失败: " + e.getMessage());
        }
    }

    @PostMapping("/crawl-movies")
    public Result<Map<String, Integer>> crawlMovies(@RequestParam(defaultValue = "250") int pages) {
        try {
            Map<String, Integer> stats = crawlService.crawlMovies(pages);
            return Result.success("爬取完成", stats);
        } catch (Exception e) {
            return Result.error(500, "爬取失败: " + e.getMessage());
        }
    }

    @PostMapping("/fetch-posters")
    public Result<Map<String, Integer>> fetchPosters() {
        try {
            Map<String, Integer> stats = crawlService.fetchPosters();
            return Result.success("海报下载完成", stats);
        } catch (Exception e) {
            return Result.error(500, "海报下载失败: " + e.getMessage());
        }
    }

    @PostMapping("/import-csv")
    public Result<Map<String, Integer>> importCsv() {
        try {
            Map<String, Integer> stats = crawlService.importFromCsv();
            return Result.success("CSV导入完成", stats);
        } catch (Exception e) {
            return Result.error(500, "CSV导入失败: " + e.getMessage());
        }
    }
}
