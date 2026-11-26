package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.StudyDashboardResponse;
import com.greenproject.classway.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ClassWay 클래스 학습 API", description = "클래스 학습 페이지(강의실) API 모듈입니다.")
@RestController
@RequestMapping("/study")
@AllArgsConstructor
public class StudyController {
    private final StudyService studyService;

    @Operation(summary = "학습 대시보드 조회 API", description = "구매자, 클래스 강사, 관리자만. 클래스 정보, 챕터별 강의 목록(온라인), 공지/자료 목록")
    @GetMapping("/{classId}")
    public ResponseEntity<ApiResponse<StudyDashboardResponse>> getDashboard(@PathVariable Integer classId) {
        StudyDashboardResponse result = studyService.getDashboard(classId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}