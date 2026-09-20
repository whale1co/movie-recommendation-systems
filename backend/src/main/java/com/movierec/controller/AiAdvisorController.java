package com.movierec.controller;

import com.movierec.ai.service.AiAdvisorService;
import com.movierec.common.ApiResponse;
import com.movierec.dto.request.AiAdvisorRequest;
import com.movierec.dto.response.AiAdvisorResponse;
import com.movierec.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 选片顾问")
@RestController
@RequestMapping("/api/v1/ai/advisor")
public class AiAdvisorController {
    private final AiAdvisorService advisorService;

    public AiAdvisorController(AiAdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @Operation(summary = "根据自然语言需求从本地片库推荐电影")
    @PostMapping
    @PreAuthorize("hasAuthority('ai:chat')")
    public ApiResponse<AiAdvisorResponse> advise(@AuthenticationPrincipal User currentUser,
                                                  @Valid @RequestBody AiAdvisorRequest request) {
        return ApiResponse.success("推荐成功", advisorService.advise(currentUser.getId(), request.question()));
    }
}
