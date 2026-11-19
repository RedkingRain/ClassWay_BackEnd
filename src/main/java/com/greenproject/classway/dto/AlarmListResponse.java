package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 알림 목록 응답 (GET /alarms) — MAIN-001 헤더 알림
@Getter
@Builder
@AllArgsConstructor
public class AlarmListResponse {
    private final List<AlarmItem> alarms; // 최신 알림 (최대 20개)
    private final Long unreadCount;       // 안 읽은 알림 전체 개수 → 종 아이콘 배지

    // 알림 한 줄
    @Getter
    @Builder
    @AllArgsConstructor
    public static class AlarmItem {
        private final Integer alarmId;
        private final String type;              // CLASS(수업), PAYMENT(결제), QNA(문의), NOTICE(안내)
        private final String title;             // 알림 내용
        private final String linkUrl;           // 누르면 이동할 주소 (없으면 null)
        private final Boolean isRead;           // 읽었는지
        private final LocalDateTime createdAt;  // 알림 시각
    }
}