package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.MyProfileResponse;
import com.greenproject.classway.dto.MyProfileUpdateRequest;
import com.greenproject.classway.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 API 요청을 받는 창구
 * - 마이페이지 API 주소는 /user/... 로 통일
 */
@Tag(name = "ClassWay 마이페이지 API", description = "마이페이지 관련 API 모듈입니다.")   // Swagger 화면의 그룹 이름
@RestController             // 이 클래스의 메서드가 돌려주는 값을 JSON으로 바꿔서 응답
@RequestMapping("/user")    // 이 클래스의 모든 API 주소 앞에 /user 붙음
@AllArgsConstructor         // final 필드(UserService)를 Spring이 자동으로 넣어줌
public class UserController {

    private final UserService userService;   // 실제 처리는 Service에게 맡김

    // ==================================================
    // 회원 정보 조회 (MY-001)
    // ==================================================
    @Operation(summary = "회원 정보 조회 API",
            description = "마이페이지 회원정보 관리 화면에 표시할 프로필 이미지, 이메일, 이름, 닉네임, 연락처, 역할, 가입 방식을 반환")
    @GetMapping("/info")   // GET /user/info 요청이 오면 이 메서드 실행 (로그인 필요)
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile() {
        // 1. Service에 조회 요청 (로그인 사용자 확인은 Service가 SecurityContext로 처리)
        // ※ 토큰이 없거나 잘못된 요청은 여기 오기 전에 보안 필터가 401로 막음
        MyProfileResponse result = userService.getMyProfile();

        // 2. 팀 공통 응답 형식 {success, message, data}로 감싸서 200 ok
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // =================================================
    // 회원 정보 수정 (MY-001)
    // =================================================

    @Operation(summary = "회원 정보 수정 API",
            description = "로그인한 사용자의 닉네임, 연락처를 수정 " +
                    "(닉네임 2~10자·중복 불가, 연락처 010-1234-5678 또는 숫자만 입력, 저장은 숫자만)")
    @PatchMapping("/infoedit") // PATCH /user/infoedit: 일부 값만 수정하는 요청(로그인 필요)
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @Valid @RequestBody MyProfileUpdateRequest request // JSON -> DTO 변환 + 형식 검사(실패 시 400)
    ) {
        // 1. service에 수정 요청 (닉네임 중복이면 409)
        userService.updateMyProfile(request);

        // 2. 성공 응답(돌려줄 데이터 없음)
        return ResponseEntity.ok(ApiResponse.ok());
    }

}