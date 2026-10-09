package com.movierec.controller;

import com.movierec.ai.service.AiAdvisorService;
import com.movierec.common.ApiResponse;
import com.movierec.common.PageResponse;
import com.movierec.dto.request.AiAdvisorRequest;
import com.movierec.dto.response.AiAdvisorHistoryDetail;
import com.movierec.dto.response.AiAdvisorHistorySummary;
import com.movierec.dto.response.AiAdvisorResponse;
import com.movierec.entity.User;
import com.movierec.service.AiAdvisorHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 选片顾问")
@RestController
@Validated
@RequestMapping("/api/v1/ai/advisor")
public class AiAdvisorController {
    private final AiAdvisorService advisorService;
    private final AiAdvisorHistoryService historyService;

    public AiAdvisorController(AiAdvisorService advisorService, AiAdvisorHistoryService historyService) {
        this.advisorService = advisorService;
        this.historyService = historyService;
    }

    @Operation(summary = "根据自然语言需求从本地片库推荐电影")
    @PostMapping
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<AiAdvisorResponse> advise(@AuthenticationPrincipal User currentUser,
                                                  @Valid @RequestBody AiAdvisorRequest request) {
        return ApiResponse.success("推荐成功", advisorService.advise(currentUser.getId(), request.question()));
    }

    @Operation(summary = "分页查看本人的 AI 选片历史")
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<PageResponse<AiAdvisorHistorySummary>> history(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) long size) {
        return ApiResponse.success("查询成功", historyService.list(currentUser.getId(), page, size));
    }

    @Operation(summary = "查看本人的 AI 选片历史详情")
    @GetMapping("/history/{id}")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<AiAdvisorHistoryDetail> historyDetail(
            @AuthenticationPrincipal User currentUser, @PathVariable @Positive Long id) {
        return ApiResponse.success("查询成功", historyService.get(currentUser.getId(), id));
    }

    @Operation(summary = "删除本人的 AI 选片历史")
    @DeleteMapping("/history/{id}")
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<Void> deleteHistory(
            @AuthenticationPrincipal User currentUser, @PathVariable @Positive Long id) {
        historyService.delete(currentUser.getId(), id);
        return ApiResponse.success("删除成功", null);
    }
}
