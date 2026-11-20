package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 카테고리 목록 응답 (GET /categories) - 헤더 카테고리 메뉴, 클래스 목록 카테고리
@Getter
@AllArgsConstructor
public class CategoryResponse {
    private final Integer categoryId;
    private final String name;
    private final String iconUrl; // 아이콘 이미지 주소 (없으면 null)
    private final Long classCount; // 공개(PUBLISHED) 클래스 수 (DB에 저장X, 쿼리로 셈)
}
