package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.dto.request.LoginRequest;
import com.movierec.dto.request.RegisterRequest;
import com.movierec.dto.response.LoginResponse;
import com.movierec.dto.response.RegisterResponse;
import com.movierec.entity.User;
import com.movierec.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "注册用户")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username().trim(), request.password(), request.preferences());
        ApiResponse<RegisterResponse> response =
                ApiResponse.created("注册成功", new RegisterResponse(user.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.username().trim(), request.password());
        return ApiResponse.success("登录成功", new LoginResponse(token));
    }
}
