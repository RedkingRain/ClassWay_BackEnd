package com.greenproject.classway.controller;

import com.greenproject.classway.dto.*;
import com.greenproject.classway.service.ClassQnaService;
import com.greenproject.classway.service.ClassService;
import com.greenproject.classway.service.WishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final WishService wishService;
    private final ClassQnaService classQnaService;

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

    @Operation(summary = "찜하기 버튼 API", description = "로그인 회원만. 찜 안 했으면 추가, 했으면 해제 후 찜 여부와 찜 수 반환")
    @PostMapping("/{classId}/wish")
    public ResponseEntity<ApiResponse<WishToggleResponse>> toggleWish(
            @PathVariable Integer classId,
            @AuthenticationPrincipal UserDetails userDetails) {

        WishToggleResponse result = wishService.toggleWish(classId, userDetails.getUsername());
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

    @Operation(summary = "Q&A 목록 조회 API", description = "삭제 안 된 문의 최신순. 비밀글은 작성자, 그 클래스 강사, 관리자만 질문과 답변을 받음")
    @GetMapping("/{classId}/qnas")
    public ResponseEntity<ApiResponse<ClassQnaListResponse>> getQnas(
            @PathVariable Integer classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 비로그인도 볼 수 있어서 userDetails가 null일 수 있음
        String loginEmail = (userDetails != null) ? userDetails.getUsername() : null;

        ClassQnaListResponse result = classQnaService.getQnas(classId, loginEmail, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Q&A 문의 등록 API", description = "로그인 회원만. question 1000자까지, isSecret 비밀글 여부")
    @PostMapping("/{classId}/qnas")
    public ResponseEntity<ApiResponse<Integer>> createQna(
            @PathVariable Integer classId,
            @Valid @RequestBody ClassQnaCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Integer qnaId = classQnaService.createQna(classId, userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(qnaId));
    }


    @Operation(summary = "일정 목록 조회 API", description = "오프라인 클래스의 예약 가능, 마감 일정 (지금 이후, 시작 빠른 순). 온라인은 빈 목록")
    @GetMapping("/{classId}/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedules(@PathVariable Integer classId) {
        List<ScheduleResponse> result = classService.getSchedules(classId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "클래스 목록, 검색 API",
            description = "공개 클래스 목록. keyword(제목, 소개, 강사 닉네임), categoryId, classType(ONLINE / OFFLINE), region(시/도, 오프라인만) / sort: latest, popular, rating, random(메인)")
    @GetMapping
    public ResponseEntity<ApiResponse<ClassListResponse>> getClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String classType,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        ClassListResponse result = classService.getClasses(keyword, categoryId, classType, region, page, size, sort);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "인기 클래스 조회 API", description = "최근 30일 예약 수 TOP 10 (취소 제외, 공개 클래스만)")
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<ClassCardResponse>>> getPopularClasses() {
        List<ClassCardResponse> result = classService.getPopularClasses();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "이벤트 클래스 조회 API", description = "노출 중인 이벤트 배지가 있는 공개 클래스 (마감 임박순, 최대 8개)")
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<ClassCardResponse>>> getEventClasses() {
        List<ClassCardResponse> result = classService.getEventClasses();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
