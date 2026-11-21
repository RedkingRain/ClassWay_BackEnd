package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 관리자 페이지 강사 승인 화면 - 탭 숫자 응답 (GET /admin/tutors/counts) - ADMIN-002
 * - 신청이 0건인 상태도 0으로 내려감 (null 아님)
 */
@AllArgsConstructor
@Getter
public class AdminTutorCountResponse {
    private final long pending;   // 승인 대기 건수
    private final long approved;  // 승인 건수
    private final long rejected;  // 반려 건수
}
