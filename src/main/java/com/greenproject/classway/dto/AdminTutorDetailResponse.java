package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 페이지 강사 신청 상세 응답 (GET /admin/tutors/{tutorId}) - ADMIN-002
 * - users(이름·이메일·연락처) + tutors(신청 정보) + tutors_career_files(제출 자료)를 합친 것
 * - 반려 사유, 처리 시간은 처리 전이면 null
 */
@AllArgsConstructor
@Getter
public class AdminTutorDetailResponse {
    private final Integer tutorId;                 // 신청 번호
    private final String name;                     // 이름 (users)
    private final String email;                    // 이메일 (users)
    private final String phone;                    // 연락처 (users)
    private final LocalDateTime appliedAt;         // 신청일 (tutors)
    private final String career;                   // 강사 경력 (tutors)
    private final List<TutorFileResponse> files;   // 제출 자료 목록 (없으면 빈 목록 [])
    private final String approvalStatus;           // 상태 ("PENDING"/"APPROVED"/"REJECTED")
    private final String rejectReason;             // 반려 사유 (반려일 때만, 아니면 null)
    private final LocalDateTime approvedAt;       // 처리(승인·반려) 시간 (처리 전이면 null)
}
