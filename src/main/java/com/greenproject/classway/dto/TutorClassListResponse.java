package com.greenproject.classway.dto;

import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TutorClassListResponse {
    private long totalCount;       // 전체 등록 강의 수 (시안 상단 "전체 14개")
    private int totalPages;        // 전체 페이지 수
    private int currentPage;       // 현재 페이지 번호 (0부터 시작)
    private List<ClassItem> classes;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClassItem {
        private Integer classId;
        private ClassType classType;       // ONLINE / OFFLINE
        private ClassStatus status;        // PUBLISHED, HIDDEN, PENDING, STOPPED 등
        private String title;              // 클래스 제목
        private String thumbnailUrl;       // 썸네일 이미지 URL
        private boolean canToggle;         // 토글 스위치 활성화 가능 여부 (PENDING, STOPPED는 false)

        private OfflineInfo offlineInfo;   // 오프라인 전용 집계 정보 (온라인이면 null)
        private OnlineInfo onlineInfo;     // 온라인 전용 집계 정보 (오프라인이면 null)
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OfflineInfo {
        private long scheduleCount;        // 회차 수 (예: 4개)
        private long reservedCount;        // 총 예약자 수 (예: 18명)
        private long capacity;             // 총 정원 수 (예: 24명)
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OnlineInfo {
        private long lectureCount;         // VOD 강의 수 (예: 24강)
        private long studentCount;         // 수강생 수 (예: 132명)
    }
}
