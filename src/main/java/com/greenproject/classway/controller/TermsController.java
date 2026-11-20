package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.TermsResponse;
import com.greenproject.classway.service.TermsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ClassWay 약관 관련 API", description = "약관 조회 API 모듈입니다.")
@RestController
@RequestMapping("/terms")
@AllArgsConstructor
public class TermsController {
    private final TermsService termsService;

    @Operation(summary = "약관 조회 API", description = "약관 유형별 현재 효력이 발생한 최신 약관 단건 조회")
    @GetMapping("/{terms_type}")
    public ResponseEntity<ApiResponse<TermsResponse>> getTerms(
            @PathVariable("terms_type") String termsType
    ) {
        TermsResponse result = termsService.getLatestTerms(termsType);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
