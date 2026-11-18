package com.greenproject.classway.controller;

import com.greenproject.classway.dto.AdminTutorListResponse;
import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.global.TutorApprovalStatus;
import com.greenproject.classway.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 페이지 API 요청을 받는 창구
 * - 관리자 API 주소는 /admin/... 으로 통일
 * - 관리자 검사는 Service에서- (아니먄 403), 로그인 검사는 JWT 필터에서- (아니먄 401)
 */
@Tag(name = "ClassWay 관리자 API", description = "관리자 페이지 관련  API 모듈입니다.")
@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {
    private final AdminService adminService;

    //======================================
    // 강사 신청 목록 조회 (ADMIN-002)
    //======================================
    @Operation(summary = "강사 신청 목록 조회 API",
            description = "승인 상태(PENDING/ APPROVED/ REJECTED)별 강사 신청 목록, 상태 생략 시 PENDING. 관리자만 가능")
    @GetMapping("/tutors")
    public ResponseEntity<ApiResponse<List<AdminTutorListResponse>>>getTutorApplication(
            @RequestParam(defaultValue = "PENDING") TutorApprovalStatus status) {
        // 1. Service에 목록 조회 요청
        List<AdminTutorListResponse>  result = adminService.getTutorApplications(status);

        // 2. 성공 시 200 ok
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
