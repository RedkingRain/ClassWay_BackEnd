package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.MainReviewResponse;
import com.greenproject.classway.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ClassWay 후기 API", description = "메인 후기 조회 API 모듈입니다.")
@RestController
@RequestMapping("/reviews")
@AllArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @Operation(summary = "메인 후기 목록 조회 API", description = "공개 클래스의 5점 후기 중 최신 3개 (숨김, 삭제 제외)")
    @GetMapping("/main")
    public ResponseEntity<ApiResponse<List<MainReviewResponse>>> getMainReviews() {
        List<MainReviewResponse> result = reviewService.getMainReviews();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}