package com.greenproject.classway.controller;

import com.greenproject.classway.dto.*;
import com.greenproject.classway.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.AllArgsConstructor;

@Tag(name="ClassWay 회원 계정 관련 API", description = "회원 계정 관리 API 모듈입니다.")
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입 API", description = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup() {
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "로그인 API", description = "패스워드 기반 로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        LoginResponse result = authService.login(loginRequest);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "로그아웃 API", description = "로그아웃하여 사용중인 엑세스 토큰 blacklist 관리 및 리프레시 토큰 삭제")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorization
    ) {
        String accessToken = authorization.substring(7);
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "엑세스 토큰 재발급 API", description = "엑세스 토큰의 짧은 만료 주기로 강제 로그아웃 방지를 위해 리프레시 토큰이 살아 있을 동안 엑세스 토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @Valid @RequestBody RefreshRequest refreshRequest
    ) {
        // 리프레시 토큰을 이용해 새로운 엑세스 토큰을 재발급 받는 API
        RefreshResponse response = authService.refresh(refreshRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
