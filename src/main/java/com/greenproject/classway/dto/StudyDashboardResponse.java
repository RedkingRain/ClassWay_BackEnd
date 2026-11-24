package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 학습 대시보드 응답 (GET /study/{classId})
@Getter
@Builder
@AllArgsConstructor
public class StudyDashboardResponse {
    private final Integer classId;
    private final String title;
    private final String classType;            // ONLINE / OFFLINE
    private final String thumbnailUrl;
    private final String tutorNickname;        // 강사 이메일은 보내지 않음
    private final String address;              // 오프라인만
    private final LocalDateTime nextScheduleAt; // 오프라인 수강생의 가장 가까운 회차 (강사, 관리자, 남은 회차 없음은 null)
    private final Boolean isTutor;             // 이 클래스를 만든 강사인지 (관리자는 false)
    private final List<ChapterInfo> chapters;  // 온라인만, 오프라인은 빈 목록
    private final List<PostInfo> posts;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ChapterInfo {
        private final Integer chapterId;
        private final String title;
        private final List<LectureInfo> lectures;
    }

    // 영상 주소는 강의 화면에서만 내려줌
    @Getter
    @Builder
    @AllArgsConstructor
    public static class LectureInfo {
        private final Integer lectureId;
        private final String title;
        private final Integer durationMinutes;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PostInfo {
        private final Integer postId;
        private final String type;             // NOTICE / MATERIAL
        private final String title;
        private final LocalDateTime createdAt;
        private final Integer viewCount;
    }
}