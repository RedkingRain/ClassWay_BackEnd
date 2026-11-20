package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.BannerResponse;
import com.greenproject.classway.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ClassWay 배너 API", description = "메인 홍보 배너 조회 API 모듈입니다.")
@RestController
@RequestMapping("/banners")
@AllArgsConstructor
public class BannerController {
    private final BannerService bannerService;

    @Operation(summary = "메인 배너 목록 조회 API", description = "노출 중(켜짐, 기간 안)인 배너, 순서 값 작은 순")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getBanners() {
        List<BannerResponse> result = bannerService.getBanners();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
