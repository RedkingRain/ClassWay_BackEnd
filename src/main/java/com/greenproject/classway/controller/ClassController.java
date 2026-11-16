package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.ClassDetailResponse;
import com.greenproject.classway.service.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ClassWay 클래스 API", description = "클래스 상세 등 클래스 조회 API 모듈입니다.")
@RestController
@RequestMapping("/classes")
@AllArgsConstructor
public class ClassController {
    private final ClassService classService;

    @Operation(summary = "클래스 상세 조회 API", description = "공개 클래스의 상세 정보 / 강사 / 커리큘럼 (로그인 시 수강 여부 포함)")
    @GetMapping("/{classId}")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> getClassDetail(
            @PathVariable Integer classId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 로그인했으면 토큰에 담긴 이메일, 비로그인이면 null
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;

        ClassDetailResponse result = classService.getClassDetail(classId, loginEmail);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
