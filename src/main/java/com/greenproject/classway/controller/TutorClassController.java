package com.greenproject.classway.controller;

import com.greenproject.classway.dto.*;
import com.greenproject.classway.service.TutorClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tutor 클래스 관리 API", description = "강사 마이페이지의 클래스 등록, 수정, 조회 및 상태 변경 API")
@RestController
@RequestMapping("/tutor")
@RequiredArgsConstructor
public class TutorClassController {

    private final TutorClassService tutorClassService;

    @Operation(summary = "강사 등록 클래스 목록 조회 API", description = "강사가 등록한 클래스 목록을 6개씩 페이징 조회합니다.")
    @GetMapping("/classes")
    public ResponseEntity<ApiResponse<TutorClassListResponse>> getTutorClasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;
        TutorClassListResponse response = tutorClassService.getTutorClasses(loginEmail, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "클래스 상태 변경 API", description = "강사가 소유한 클래스를 오픈(PUBLISHED) 또는 비활성(HIDDEN)으로 변경합니다.")
    @PatchMapping("/{classId}/status")
    public ResponseEntity<ApiResponse<Void>> changeClassStatus(
            @PathVariable Integer classId,
            @Valid @RequestBody ClassStatusChangeRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;
        tutorClassService.updateClassStatus(classId, request.getStatus(), loginEmail);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "클래스 등록 API", description = "강사가 신규 클래스와 커리큘럼(챕터·강의)을 등록합니다. 초기 상태는 DRAFT로 저장됩니다.")
    @PostMapping(value = "/class_create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Integer>> createClass(
            @Valid @ModelAttribute TutorClassCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;
        Integer classId = tutorClassService.createClass(request, loginEmail);
        return ResponseEntity.ok(ApiResponse.ok(classId));
    }

    @Operation(summary = "수정할 클래스 데이터 조회 API", description = "클래스 수정 화면에 표시할 기본 정보 및 상세 소개 데이터를 조회합니다.")
    @GetMapping("/{classId}")
    public ResponseEntity<ApiResponse<TutorClassDetailResponse>> getTutorClassDetail(
            @PathVariable Integer classId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;
        TutorClassDetailResponse response = tutorClassService.getTutorClassDetail(classId, loginEmail);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "커리큘럼 조회 API", description = "클래스 수정 화면에 표시할 챕터 및 강의 목록을 조회합니다.")
    @GetMapping("/{classId}/chapter")
    public ResponseEntity<ApiResponse<TutorCurriculumResponse>> getClassCurriculum(
            @PathVariable Integer classId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;
        TutorCurriculumResponse response = tutorClassService.getClassCurriculum(classId, loginEmail);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "커리큘럼 저장 API", description = "클래스의 챕터 및 강의 목록을 추가, 수정, 삭제합니다.")
    @PatchMapping("/{classId}/chapter")
    public ResponseEntity<ApiResponse<Void>> saveClassCurriculum(
            @PathVariable Integer classId,
            @Valid @RequestBody TutorCurriculumSaveRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;
        tutorClassService.saveClassCurriculum(classId, request, loginEmail);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "클래스 수정 저장 API", description = "클래스 상태(DRAFT, PUBLISHED, HIDDEN)별 허용된 항목을 검증하고 수정 저장합니다.")
    @PatchMapping(value = "/{classId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateClass(
            @PathVariable Integer classId,
            @Valid @ModelAttribute TutorClassUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;
        tutorClassService.updateClass(classId, request, loginEmail);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
