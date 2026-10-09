package com.movierec.controller;

import com.movierec.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success("服务正常", Map.of("status", "OK"));
    }
}
