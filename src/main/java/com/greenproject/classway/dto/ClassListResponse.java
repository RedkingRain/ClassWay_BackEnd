package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor

// 클래스 목록 응답 (GET /classes) - 메인 전체 클래스, 검색/카테고리 목록
public class ClassListResponse {
    private final List<ClassCardResponse> classes; // 이번 페이지 카드
    private final Long totalCount; // 조건에 맞는 전체 개수 -> [검색 결과 N개]
    private final Integer totalPages; // 전체 페이지 수 -> 페이지 번호 버튼
}
