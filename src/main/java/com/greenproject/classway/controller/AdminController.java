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
 * - 관리자 검사는 Service에서- (아니면 403), 로그인 검사는 JWT 필터에서- (아니면 401)
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
    public ResponseEntity<ApiResponse<List<AdminTutorListResponse>>> getTutorApplications(
            @RequestParam(defaultValue = "PENDING") TutorApprovalStatus status) {
        // 1. Service에 목록 조회 요청
        List<AdminTutorListResponse>  result = adminService.getTutorApplications(status);

        // 2. 성공 시 200 ok
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    // =============================
    // 강사 승인 (ADMIN-002)
    // =============================
    @Operation(summary = "강사 승인 API",
    description = "승인 대기 중인 강사 신청을 승인, 이미 처리된 신청이면 400, 없는 신청이면 404. 관리자만 가능")
    @PatchMapping("/tutors/{tutorId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveTutor(
            @PathVariable Integer tutorId
    ) {
        // 1. Service에 승인 요청 (관리자 확인, 상태 확인, 저장은 Service가)
        adminService.approveTutor(tutorId);

        // 2. 성공 응답
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
