package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.common.PageResponse;
import com.movierec.dto.UserProfileVO;
import com.movierec.dto.SecurityAuditEventVO;
import com.movierec.dto.AdminMovieVO;
import com.movierec.dto.request.AdminUserCreateRequest;
import com.movierec.dto.request.AdminUserUpdateRequest;
import com.movierec.dto.request.AdminMovieUpdateRequest;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.mapper.CrawlLogMapper;
import com.movierec.entity.User;
import com.movierec.service.UserService;
import com.movierec.service.SecurityAuditService;
import com.movierec.service.MovieService;
import com.movierec.service.CrawlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "管理")
@Validated
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final CrawlService crawlService;
    private final MovieMapper movieMapper;
    private final UserMapper userMapper;
    private final RatingMapper ratingMapper;
    private final UserService userService;
    private final SecurityAuditService auditService;
    private final CrawlLogMapper crawlLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MovieService movieService;

    @org.springframework.beans.factory.annotation.Value("${movie.poster-dir:./posters}")
    private String posterDir;

    public AdminController(CrawlService crawlService, MovieMapper movieMapper, UserMapper userMapper,
                           RatingMapper ratingMapper, UserService userService,
                           SecurityAuditService auditService, CrawlLogMapper crawlLogMapper,
                           JdbcTemplate jdbcTemplate, MovieService movieService) {
        this.crawlService = crawlService;
        this.movieMapper = movieMapper;
        this.userMapper = userMapper;
        this.ratingMapper = ratingMapper;
        this.userService = userService;
        this.auditService = auditService;
        this.crawlLogMapper = crawlLogMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.movieService = movieService;
    }

    @Operation(summary = "查看系统统计")
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "OK");
        status.put("service", "movie-rec-backend");
        status.put("movieCount", movieMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.movierec.entity.Movie>().eq(com.movierec.entity.Movie::getStatus, "ACTIVE")));
        status.put("userCount", userMapper.selectCount(null));
        status.put("ratingCount", ratingMapper.selectCount(null));
        Object averageRating = ratingMapper.selectObjs(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.movierec.entity.Rating>().select("AVG(score)")).stream().findFirst().orElse(null);
        status.put("averageRating", averageRating == null ? 0 : averageRating);
        status.put("newMoviesToday", movieMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.movierec.entity.Movie>().eq(com.movierec.entity.Movie::getStatus, "ACTIVE").ge(com.movierec.entity.Movie::getCreateTime, LocalDateTime.of(LocalDate.now(), LocalTime.MIN))));
        status.put("newUsersToday", userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>().ge(User::getCreateTime, LocalDateTime.of(LocalDate.now(), LocalTime.MIN))));
        status.put("runningTaskCount", crawlLogMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.movierec.entity.CrawlLog>().eq(com.movierec.entity.CrawlLog::getStatus, "RUNNING")));
        status.put("failedTaskCount", crawlLogMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.movierec.entity.CrawlLog>().eq(com.movierec.entity.CrawlLog::getStatus, "FAILED")));
        status.put("recentTasks", crawlLogMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.movierec.entity.CrawlLog>().orderByDesc(com.movierec.entity.CrawlLog::getCreateTime).last("LIMIT 5")).stream().map(task -> Map.of("id", task.getId(), "taskType", task.getTaskType(), "status", task.getStatus(), "message", task.getMessage() == null ? "" : task.getMessage(), "createTime", task.getCreateTime())).toList());
        return ApiResponse.success("系统运行正常", status);
    }

    @Operation(summary = "查看系统健康状态")
    @GetMapping("/system/health")
    public ApiResponse<Map<String, Object>> systemHealth() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("application", "UP");
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "UP");
        } catch (RuntimeException ex) {
            status.put("database", "DOWN");
        }
        Path storage = Path.of(posterDir).toAbsolutePath().normalize();
        status.put("storage", Files.isDirectory(storage) && Files.isWritable(storage) ? "UP" : "DOWN");
        if ("UP".equals(status.get("database"))) {
            status.put("runningTasks", crawlLogMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.movierec.entity.CrawlLog>().eq(com.movierec.entity.CrawlLog::getStatus, "RUNNING")));
        } else {
            status.put("runningTasks", null);
        }
        status.put("serverTime", LocalDateTime.now());
        status.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        return ApiResponse.success("健康状态查询成功", status);
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('user:read')")
    public ApiResponse<PageResponse<UserProfileVO>> users(
            @Min(1) @RequestParam(defaultValue = "1") long current,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        return ApiResponse.success("查询成功", userService.listAdminUsers(current, size, keyword, status, role));
    }

    @Operation(summary = "分页查询电影")
    @GetMapping("/movies")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('movie:read')")
    public ApiResponse<PageResponse<AdminMovieVO>> movies(
            @Min(1) @RequestParam(defaultValue = "1") long current,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String genre,
            @Min(1880) @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status) {
        return ApiResponse.success("查询成功", movieService.listAdminMovies(current, size, keyword, genre, year, status));
    }

    @Operation(summary = "查看电影管理详情")
    @GetMapping("/movies/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('movie:read')")
    public ApiResponse<AdminMovieVO> movie(@Positive @PathVariable Long id) {
        return ApiResponse.success("查询成功", movieService.getAdminMovie(id));
    }

    @Operation(summary = "修改电影")
    @PatchMapping("/movies/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('movie:write')")
    public ApiResponse<AdminMovieVO> updateMovie(@AuthenticationPrincipal User operator,
                                                   @Positive @PathVariable Long id,
                                                   @Valid @RequestBody AdminMovieUpdateRequest request) {
        AdminMovieVO updated = movieService.updateAdminMovie(id, request);
        auditService.record("ADMIN_MOVIE_UPDATE", "SUCCESS", operator.getId(), String.valueOf(id), null);
        return ApiResponse.success("修改成功", updated);
    }

    @Operation(summary = "逻辑删除电影")
    @DeleteMapping("/movies/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('movie:write')")
    public ApiResponse<Void> deleteMovie(@AuthenticationPrincipal User operator, @Positive @PathVariable Long id) {
        movieService.deleteAdminMovie(id);
        auditService.record("ADMIN_MOVIE_DELETE", "SUCCESS", operator.getId(), String.valueOf(id), null);
        return ApiResponse.success("电影已移入回收状态", null);
    }

    @Operation(summary = "查看用户详情")
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('user:read')")
    public ApiResponse<UserProfileVO> user(@Positive @PathVariable Long id) {
        return ApiResponse.success("查询成功", userService.getAdminUser(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<UserProfileVO>> createUser(@AuthenticationPrincipal User operator,
                                                                  @Valid @RequestBody AdminUserCreateRequest request) {
        UserProfileVO created = userService.createAdminUser(request);
        auditService.record("ADMIN_USER_CREATE", "SUCCESS", operator.getId(),
                String.valueOf(created.getId()), "role=" + created.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("创建成功", created));
    }

    @Operation(summary = "修改用户角色、状态或资料")
    @PatchMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('user:write')")
    public ApiResponse<UserProfileVO> updateUser(@AuthenticationPrincipal User operator,
                                                  @Positive @PathVariable Long id,
                                                  @Valid @RequestBody AdminUserUpdateRequest request) {
        UserProfileVO updated = userService.updateAdminUser(operator.getId(), id, request);
        auditService.record("ADMIN_USER_UPDATE", "SUCCESS", operator.getId(), String.valueOf(id), null);
        return ApiResponse.success("修改成功", updated);
    }

    @Operation(summary = "注销用户")
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('user:write')")
    public ApiResponse<Void> deleteUser(@AuthenticationPrincipal User operator, @Positive @PathVariable Long id) {
        userService.deleteAdminUser(operator.getId(), id);
        auditService.record("ADMIN_USER_DELETE", "SUCCESS", operator.getId(), String.valueOf(id), null);
        return ApiResponse.success("注销成功", null);
    }

    @Operation(summary = "分页查询安全审计事件")
    @GetMapping("/audit-events")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('audit:read')")
    public ApiResponse<PageResponse<SecurityAuditEventVO>> auditEvents(
            @Min(1) @RequestParam(defaultValue = "1") long current,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success("查询成功", auditService.list(current, size));
    }

    @Operation(summary = "下载海报")
    @PostMapping("/fetch-posters")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('crawl:execute')")
    public ApiResponse<Map<String, Integer>> fetchPosters() { return ApiResponse.success("海报下载完成", crawlService.fetchPosters()); }

    @Operation(summary = "导入 CSV")
    @PostMapping("/import-csv")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('crawl:execute')")
    public ApiResponse<Map<String, Integer>> importCsv() { return ApiResponse.success("CSV导入完成", crawlService.importFromCsv()); }
}
