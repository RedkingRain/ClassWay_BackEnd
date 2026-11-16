package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 수강평 목록 API 응답 (GET /api/classes/{classId}/reviews) — CLASS-001 수강평 탭
// 프론트 mocks/reviewData.js의 한 줄 모양과 같음
@Getter
@Builder
@AllArgsConstructor
public class ReviewListResponse {
    private final List<ReviewItem> reviews; // 이번 페이지 수강평
    private final Long totalCount;          // 보이는 수강평 전체 개수 (숨김 · 삭제 제외)
    private final Boolean hasNext;          // 다음 페이지가 있는지 → 「후기 더보기」 버튼 표시

    // 수강평 한 줄 — 작성자는 닉네임만 (실명 · 이메일은 보내지 않음)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReviewItem {
        private final Integer reviewId;          // = 예약 번호 (예약 1건당 수강평 1개)
        private final String writerNickname;
        private final Byte rating;               // 별점 1 ~ 5
        private final String content;
        private final LocalDateTime attendedAt;  // 수강일 (오프라인 수업 시작 일시, 온라인은 null)
        private final LocalDateTime createdAt;   // 작성일
    }
}