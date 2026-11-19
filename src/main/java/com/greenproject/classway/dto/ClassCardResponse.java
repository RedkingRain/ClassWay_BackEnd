package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// 클래스 카드 한 장 — 메인 인기 · 이벤트 · 전체 클래스, 검색 결과에서 공통 사용 (MAIN-002 · 003)
@Getter
@Builder
@AllArgsConstructor
public class ClassCardResponse {
    private final Integer classId;
    private final String title;
    private final String thumbnailUrl;
    private final String classType;             // ONLINE / OFFLINE
    private final Integer categoryId;
    private final String categoryName;
    private final Integer price;                // 정가
    private final Integer salePrice;            // 할인가 (없으면 null)
    private final Integer discountRate;         // 할인율 % (소수점 버림, 없으면 0)
    private final BigDecimal avgRating;
    private final Integer reviewCount;
    private final Integer wishCount;
    private final Integer totalDurationMinutes; // 강의 길이 합계 (분) → [약 N시간]
    private final String tutorNickname;         // 강사 닉네임 (실명 X)
    private final String eventLabel;            // 노출 중인 이벤트 배지 문구 (없으면 null)
}