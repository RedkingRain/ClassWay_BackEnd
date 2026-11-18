package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter

public class MyInfoResponse {
    private final Integer userId; // user_id
    private final String email;   // email(ID)
    private final String name;    // user name
    private final String role;    // 권한(STUDENT, TUTOR, ADMIN 등)
    private final String nickname; // 활동명 (화면 표시용)
    private final String tutorStatus; // 강사 승인 상태 (PENDING, APPROVED, REJECTED), 강사 신청 안 했으면 null
}
