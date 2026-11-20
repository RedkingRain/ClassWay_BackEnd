package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.CategoryResponse;
import com.greenproject.classway.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "ClassWay 카테고리 API", description = "카테고리 메뉴 조회 API 모듈입니다.")
@RestController
@RequestMapping("/categories")
@AllArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "카테고리 목록 조회 API", description = "사용 중인 카테고리 (번호 순서), classCount: 공개 클래스 수")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getCategories()));
    }
}