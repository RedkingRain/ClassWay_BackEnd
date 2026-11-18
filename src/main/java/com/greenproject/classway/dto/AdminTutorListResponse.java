package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 페이지 강사 승인 화면 - 신청 목록 "한 줄"에 대한 응답 DTO (ADMIN-002)
 */
@AllArgsConstructor
@Getter
public class AdminTutorListResponse {
    private final Integer tutorId;          // 신청 번호, 상세·승인·반려 주소에 사용
    private final String name;              // 신청자 이름
    private final String email;             // 신청자 이메일
    private final LocalDateTime appliedAt;  // 신청일
    private final String approvalStatus;    // 상태 PENDING/ APPROVED/ REJECTED
}
