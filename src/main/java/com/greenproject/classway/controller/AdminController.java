package com.greenproject.classway.controller;

import com.greenproject.classway.dto.AdminTutorCountResponse;
import com.greenproject.classway.dto.AdminTutorDetailResponse;
import com.greenproject.classway.dto.AdminTutorListResponse;
import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.TutorFileDownload;
import com.greenproject.classway.dto.TutorRejectRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import jakarta.validation.Valid;
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
    //======================================
    // 강사 신청 탭별 건수 (ADMIN-002)
    // - "/tutors/counts"는 글자가 고정된 주소라 "/tutors/{tutorId}"보다 먼저 연결됨
    //======================================
    @Operation(summary = "강사 신청 탭별 건수 API",
            description = "승인 대기·승인·반려 건수 (0건도 0으로). 관리자만 가능")
    @GetMapping("/tutors/counts")
    public ResponseEntity<ApiResponse<AdminTutorCountResponse>> getTutorApplicationCounts() {
        // 1. Service에 건수 조회 요청
        AdminTutorCountResponse result = adminService.getTutorApplicationCounts();

        // 2. 성공 시 200 + 건수
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    //======================================
    // 강사 신청 상세 조회 (ADMIN-002)
    //======================================
    @Operation(summary = "강사 신청 상세 조회 API",
            description = "신청자 정보, 경력, 제출 자료(원래 파일명), 처리 결과 조회. 상태와 상관없이 조회 가능, 없는 신청이면 404. 관리자만 가능")
    @GetMapping("/tutors/{tutorId}")
    public ResponseEntity<ApiResponse<AdminTutorDetailResponse>> getTutorApplicationDetail(
            @PathVariable Integer tutorId    // 주소의 신청 번호
    ) {
        // 1. Service에 상세 조회 요청
        AdminTutorDetailResponse result = adminService.getTutorApplicationDetail(tutorId);

        // 2. 성공 시 200 + 상세 정보
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    //======================================
    // 강사 제출 자료 보기 (ADMIN-002)
    //======================================
    @Operation(summary = "강사 제출 자료 보기 API",
            description = "제출 자료 파일 1개를 바이너리로 응답 (JSON 아님). " +
                    "파일 정보가 없거나 다른 신청서 파일이면 404, 디스크에 파일이 없으면 404, " +
                    "파일의 실제 위치가 업로드 폴더 밖이면 403. 관리자만 가능")
    @GetMapping("/tutors/{tutorId}/files/{fileId}")
    public ResponseEntity<Resource> getTutorFile(
            @PathVariable Integer tutorId,  // 주소의 신청 번호
            @PathVariable Integer fileId    // 주소의 파일 번호 (상세 조회의 files[].fileId)
    ) {
        // 1. Service에서 파일 꺼내기 (관리자 확인, 소속 확인, 경로 확인은 Service가)
        TutorFileDownload file = adminService.getTutorFile(tutorId, fileId);

        // 2. 브라우저가 바로 열 수 있게(inline) + 한글 파일명이 깨지지 않게(UTF-8) 헤더 작성
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build();

        // 3. 성공 시 200 + 파일 바이너리
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.getResource());
    }
    // =============================
    // 강사 승인 (ADMIN-002)
    // =============================
    @Operation(summary = "강사 승인 API",
    description = "승인 대기 중인 강사 신청을 승인, 이미 처리된 신청이면 409, 없는 신청이면 404. 관리자만 가능")
    @PatchMapping("/tutors/{tutorId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveTutor(
            @PathVariable Integer tutorId
    ) {
        // 1. Service에 승인 요청 (관리자 확인, 상태 확인, 저장은 Service가)
        adminService.approveTutor(tutorId);

        // 2. 성공 응답
        return ResponseEntity.ok(ApiResponse.ok());
    }
    // =============================
    // 강사 반려 (ADMIN-002)
    // =============================
    @Operation(summary = "강사 반려 API",
    description = "승인 대기 중인 강사 신청을 반려. " +
            "반려 사유 필수(없으면 400), 이미 처리된 신청 409, 없는 신청 404, 관리자만 가능")
    @PatchMapping("/tutors/{tutorId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectTutor(
            @PathVariable Integer tutorId,                 // 주소의 신청 번호
            @Valid @RequestBody TutorRejectRequest request // 반려 사유 + 검증
    ) {
        // 1. Service에 반려 요청 (관리자 확인, 상태 확인, 저장은 Service가)
        adminService.rejectTutor(tutorId, request);

        // 2. 성공 응답
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
