package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.LoginRequest;
import com.greenproject.classway.dto.LoginResponse;
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

    @Operation(summary = "회원가입 API", description = "회원가입을 할 때 씀")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup() {
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "로그인 API", description = "패스워드 기반 로그인을 할 때 씀")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        LoginResponse result = authService.login(loginRequest);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "로그아웃 API", description = "로그아웃을 할 때 씀")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorization
    ) {
        String accessToken = authorization.substring(7);
        authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
