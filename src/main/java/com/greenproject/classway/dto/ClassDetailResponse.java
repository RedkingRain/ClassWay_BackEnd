package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// 클래스 상세 API 응답 (GET /classes/{classId}) — CLASS-001 ~ 003
// 프론트 mocks/classDetailData.js와 같은 모양
@Getter
@Builder
@AllArgsConstructor
public class ClassDetailResponse {
    private final Integer classId;
    private final String title;
    private final String summary;
    private final String description;          // 강사가 쓴 HTML (화면에서 DOMPurify로 한 번 더 정화)
    private final String classType;            // ONLINE / OFFLINE
    private final CategoryInfo category;
    private final String thumbnailUrl;
    private final Integer price;               // 정가
    private final Integer salePrice;           // 할인가 (할인 없으면 null)
    private final Integer discountRate;        // 할인율 % (소수점 버림, 할인 없으면 0)
    private final BigDecimal avgRating;
    private final Integer reviewCount;
    private final Integer wishCount;
    private final Long studentCount;           // 수강생 수 (예약 · 수강 완료 인원)
    private final Integer totalDurationMinutes; // 강의 길이 합계 (분)
    private final Integer lectureCount;
    private final String region;               // 오프라인만
    private final String address;              // 오프라인만
    private final Integer maxCapacity;         // 오프라인 최대 정원 (온라인은 null)
    private final Boolean isEnrolled;          // 로그인한 사람이 이미 수강 중인지 (비로그인은 false)
    private final Boolean isWished;            // 로그인한 사람이 찜했는지 (비로그인은 false)
    private final TutorInfo tutor;
    private final List<ChapterInfo> chapters;

    // 카테고리
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CategoryInfo {
        private final Integer categoryId;
        private final String name;
    }

    // 강사 — 실명 · 이메일 · 연락처는 보내지 않음 (닉네임만)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TutorInfo {
        private final Integer tutorId;
        private final String nickname;
        private final String profileImage;
        private final String intro;
        private final String career;
        private final Long classCount;         // 이 강사의 공개 클래스 수
    }

    // 챕터 (강의 묶음)
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ChapterInfo {
        private final Integer chapterId;
        private final String title;
        private final List<LectureInfo> lectures;
    }

    // 강의 — 영상 주소(video_url)는 결제 전이라 절대 넣지 않음
    @Getter
    @Builder
    @AllArgsConstructor
    public static class LectureInfo {
        private final Integer lectureId;
        private final String title;
        private final Integer durationMinutes;
    }
}