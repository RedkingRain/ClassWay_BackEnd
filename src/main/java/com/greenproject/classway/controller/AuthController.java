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

    @Operation(summary = "수강생 회원가입 API", description = "수강생의 회원가입 진행(password 기반)")
    @PostMapping("/signup/student")
    public ResponseEntity<ApiResponse<Void>> signupStudent(@Valid @ModelAttribute SignupStudentRequest signupStudentRequest) {
        authService.signupStudent(signupStudentRequest);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "강사 회원가입 API", description = "강사의 회원가입 진행(password 기반)")
    @PostMapping("/signup/tutor")
    public ResponseEntity<ApiResponse<Void>> signupTutor(@Valid @ModelAttribute SignupTutorRequest signupTutorRequest) {
        authService.signupTutor(signupTutorRequest);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "로그인 API", description = "password 기반 로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        LoginResponse result = authService.login(loginRequest);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation (summary = "현재 사용자 정보 조회 API", description = "인증된 사용자의 ID, 이메일, 이름, 역할을 반환")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getUserInfo(
            @RequestHeader("Authorization") String authorization
    ) {
        // 인증 필터를 통과한 요청의 헤더에서 "Bearer" 접두사 제거
        String accessToken = authorization.substring(7);

        // 서비스에서 현재 계정 정보를 조회
        MyInfoResponse result = authService.getUserInfo(accessToken);

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

    @Operation(summary = "닉네임 중복 확인 API", description = "닉네임이 이미 존재하는지 확인")
    @PostMapping("/nickname_duplicate_check")
    public ResponseEntity<ApiResponse<DuplicateCheckResponse>> checkNicknameDuplicate(
            @Valid @RequestBody NicknameCheckRequest nicknameCheckRequest
    ) {
        authService.checkNicknameDuplicate(nicknameCheckRequest);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "이메일 중복 확인 및 인증 API", description = "이메일이 존재하는지 확인 후 인증번호 메일 발송")
    @PostMapping("/email_check")
    public ResponseEntity<ApiResponse<Void>> checkEmail(
            @Valid @RequestBody EmailCheckRequest emailCheckRequest
    ) {
        authService.checkEmail(emailCheckRequest);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "이메일 인증코드 전송 API", description = "이메일 인증번호 메일 발송")
    @PostMapping("/email_confirm_code_send")
    public ResponseEntity<ApiResponse<Void>> checkConfirmEmail(
            @Valid @RequestBody EmailCheckRequest emailCheckRequest
    ) {
        authService.sendVerificationEmail(emailCheckRequest.getEmail());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "이메일 인증번호 확인 API", description = "발송된 이메일 인증번호 일치 여부 및 유효시간 검증")
    @PostMapping("/email_code_check")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(
            @Valid @RequestBody EmailVerifyRequest emailVerifyRequest
    ) {
        authService.verifyEmailCode(emailVerifyRequest);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
