package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 일정 목록 API 응답 한 줄 (GET /classes/{classId}/schedules) — CLASS-002 날짜 · 시간 선택
// 프론트 mocks/scheduleData.js와 같은 모양
@Getter
@Builder
@AllArgsConstructor
public class ScheduleResponse {
    private final Integer scheduleId;
    private final LocalDateTime startAt;     // 수업 시작 일시
    private final String runningTime;        // 진행 시간 "03:00:00" (끝나는 시각 = startAt + runningTime)
    private final Integer remainingSeats;    // 남은 자리 = 정원 - 예약 인원 (서버 계산)
    private final String status;             // OPEN(예약 가능) / FULL(마감)
}