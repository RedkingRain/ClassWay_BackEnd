package com.greenproject.classway.controller;

import com.greenproject.classway.dto.*;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


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

    @Operation(summary = "수강평 목록 조회 API", description = "공개 클래스의 수강평 (숨김,삭제 제외), sort: latest(최신순) / rating(별점순)")
    @GetMapping("/{classId}/reviews")
    public ResponseEntity<ApiResponse<ReviewListResponse>> getReviews(
            @PathVariable Integer classId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        ReviewListResponse result = classService.getReviews(classId, sort, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "일정 목록 조회 API", description = "오프라인 클래스의 예약 가능, 마감 일정 (지금 이후, 시작 빠른 순). 온라인은 빈 목록")
    @GetMapping("/{classId}/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedules(@PathVariable Integer classId) {
        List<ScheduleResponse> result = classService.getSchedules(classId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "클래스 목록 조회 API", description = "공개 클래스 목록 (최신 등록순), classType: ONLINE / OFFLINE / 없으면 전체")
    @GetMapping
    public ResponseEntity<ApiResponse<ClassListResponse>> getClasses(
            @RequestParam(required = false) String classType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        ClassListResponse result = classService.getClasses(classType, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}



