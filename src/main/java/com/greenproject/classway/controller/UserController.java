package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.MyProfileResponse;
import com.greenproject.classway.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 마이페이지 API 요청을 받는 창구
 * ※ 클래스에 @RequestMapping을 붙이지 않음
 *   → MY-001은 /auth/..., MY-002~005는 /my/... 주소를 쓰기 때문에 메서드마다 전체 주소를 적음
 */
@Tag(name = "ClassWay 마이페이지 API", description = "마이페이지 관련 API 모듈입니다.")   // Swagger 화면의 그룹 이름
@RestController         // 이 클래스의 메서드가 돌려주는 값을 JSON으로 바꿔서 응답
@AllArgsConstructor     // final 필드(MyPageService)를 Spring이 자동으로 넣어줌
public class UserController {

    private final UserService userService;   // 실제 처리는 Service에게 맡김

    // ==================================================
    // 회원 정보 조회 (MY-001)
    // ==================================================
    @Operation(summary = "회원 정보 조회 API",
            description = "마이페이지 회원정보 관리 화면에 표시할 프로필 이미지, 이메일, 이름, 닉네임, 연락처, 역할, 가입 방식을 반환")
    @GetMapping("/user/info")   // GET 방식으로 /auth/info 요청이 오면 이 메서드 실행 (로그인 필요)
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(
            @RequestHeader("Authorization") String authorization   // 요청 헤더의 "Bearer 토큰값"을 받음
    ) {
        // 1. 헤더 값 "Bearer eyJ..."에서 앞 7글자("Bearer ")를 잘라 토큰만 남김
        //    ※ 토큰이 없거나 잘못된 요청은 여기 오기 전에 보안 필터가 401로 막음
        String accessToken = authorization.substring(7);

        // 2. Service에게 회원 정보 조회 요청
        MyProfileResponse result = userService.getMyProfile(accessToken);

        // 3. 팀 공통 응답 형식 {success, message, data}로 감싸서 200 OK로 응답
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}