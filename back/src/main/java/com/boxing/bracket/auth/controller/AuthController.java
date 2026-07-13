package com.boxing.bracket.auth.controller;

import com.boxing.bracket.auth.dto.AuthAccountResponse;
import com.boxing.bracket.auth.dto.LoginRequest;
import com.boxing.bracket.auth.dto.LoginResponse;
import com.boxing.bracket.auth.service.AuthService;
import com.boxing.bracket.common.response.ApiResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "OK");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ApiResponse.success(null, "OK");
    }

    @GetMapping("/me")
    public ApiResponse<AuthAccountResponse> me(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.success(authService.me(authorization), "OK");
    }
}
