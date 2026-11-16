package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 서버가 만들어서 내보내기만 하는 응답이라
// private final + @AllArgsConstructor + @Getter
// MyInfoResponse와 통일

@AllArgsConstructor
@Getter
public class MyProfileResponse {
    private final String profileImage; // 프로필 이미지 경로 (없으면 null)
    private final String email;        // 이메일(ID), 수정 불가
    private final String name;         // 이름, 수정 불가
    private final String nickname;     // 닉네임
    private final String phone;        // 연락처
    private final String role;         // 권한(STUDENT, TUTOR, ADMIN)
    private final String provider;     // 가입 방식(LOCAL, KAKAO, NAVER, GOOGLE)

}
