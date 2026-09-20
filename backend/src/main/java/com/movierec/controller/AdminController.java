package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.common.PageResponse;
import com.movierec.dto.UserProfileVO;
import com.movierec.dto.SecurityAuditEventVO;
import com.movierec.dto.request.AdminUserCreateRequest;
import com.movierec.dto.request.AdminUserUpdateRequest;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.entity.User;
import com.movierec.service.CrawlService;
import com.movierec.service.UserService;
import com.movierec.service.SecurityAuditService;
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
import org.springframework.web.bind.annotation.*;

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

    public AdminController(CrawlService crawlService, MovieMapper movieMapper, UserMapper userMapper,
                           RatingMapper ratingMapper, UserService userService,
                           SecurityAuditService auditService) {
        this.crawlService = crawlService;
        this.movieMapper = movieMapper;
        this.userMapper = userMapper;
        this.ratingMapper = ratingMapper;
        this.userService = userService;
        this.auditService = auditService;
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

    @Operation(summary = "分页查询用户")
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('user:read')")
    public ApiResponse<PageResponse<UserProfileVO>> users(
            @Min(1) @RequestParam(defaultValue = "1") long current,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success("查询成功", userService.listAdminUsers(current, size));
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

    @Operation(summary = "爬取 Top 250")
    @PostMapping("/crawl")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('crawl:execute')")
    public ApiResponse<Map<String, Integer>> startCrawl() { return ApiResponse.success("爬取完成", crawlService.crawlTop250()); }

    @Operation(summary = "按页爬取电影")
    @PostMapping("/crawl-movies")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('crawl:execute')")
    public ApiResponse<Map<String, Integer>> crawlMovies(@Min(1) @Max(500) @RequestParam(defaultValue = "250") int pages) {
        return ApiResponse.success("爬取完成", crawlService.crawlMovies(pages));
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
