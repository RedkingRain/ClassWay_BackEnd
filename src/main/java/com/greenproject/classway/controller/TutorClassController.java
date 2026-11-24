package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.ClassStatusChangeRequest;
import com.greenproject.classway.dto.TutorClassListResponse;
import com.greenproject.classway.service.TutorClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tutor 클래스 관리 API", description = "강사 마이페이지의 클래스 목록 조회 및 상태 변경 API")
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
}
