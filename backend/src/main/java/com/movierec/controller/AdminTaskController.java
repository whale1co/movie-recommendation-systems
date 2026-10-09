package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.common.PageResponse;
import com.movierec.entity.AdminTask;
import com.movierec.entity.AdminTaskError;
import com.movierec.entity.User;
import com.movierec.service.AdminTaskService;
import com.movierec.service.SecurityAuditService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin/tasks")
@PreAuthorize("hasRole('ADMIN') and hasAuthority('crawl:execute')")
public class AdminTaskController {
    private final AdminTaskService taskService;
    private final SecurityAuditService auditService;

    public AdminTaskController(AdminTaskService taskService, SecurityAuditService auditService) {
        this.taskService = taskService;
        this.auditService = auditService;
    }

    @PostMapping("/import/csv")
    public ResponseEntity<ApiResponse<AdminTask>> importCsv(@AuthenticationPrincipal User operator,
                                                              @RequestPart("file") MultipartFile file) {
        AdminTask task = taskService.submitCsvImport(operator.getId(), file);
        auditService.record("ADMIN_TASK_CREATE", "SUCCESS", operator.getId(), String.valueOf(task.getId()), "type=CSV_IMPORT");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.accepted("CSV 导入任务已提交", task));
    }

    @PostMapping("/posters/download")
    public ResponseEntity<ApiResponse<AdminTask>> downloadPosters(@AuthenticationPrincipal User operator) {
        AdminTask task = taskService.submitPosterDownload(operator.getId());
        auditService.record("ADMIN_TASK_CREATE", "SUCCESS", operator.getId(), String.valueOf(task.getId()), "type=POSTER_DOWNLOAD");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.accepted("海报下载任务已提交", task));
    }

    @GetMapping("/posters/status")
    public ApiResponse<java.util.Map<String, Object>> posterStatus() {
        return ApiResponse.success("海报状态查询成功", taskService.posterStatus());
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminTask>> list(
            @Min(1) @RequestParam(defaultValue = "1") long current,
            @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType) {
        return ApiResponse.success("查询成功", taskService.list(current, size, status, taskType));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AdminTask> get(@Positive @PathVariable Long taskId) {
        return ApiResponse.success("查询成功", taskService.get(taskId));
    }

    @GetMapping("/{taskId}/errors")
    public ApiResponse<List<AdminTaskError>> errors(@Positive @PathVariable Long taskId) {
        return ApiResponse.success("查询成功", taskService.errors(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<AdminTask> cancel(@AuthenticationPrincipal User operator, @Positive @PathVariable Long taskId) {
        AdminTask task = taskService.cancel(taskId);
        auditService.record("ADMIN_TASK_CANCEL", "SUCCESS", operator.getId(), String.valueOf(taskId), null);
        return ApiResponse.success("任务已取消", task);
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal User operator, @Positive @PathVariable Long taskId) {
        taskService.delete(taskId);
        auditService.record("ADMIN_TASK_DELETE", "SUCCESS", operator.getId(), String.valueOf(taskId), null);
        return ApiResponse.success("任务已删除", null);
    }
}
