package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class LoginResponse {
    private String accessToken; // 엑세스 토큰
    private String refreshToken;
    private Integer userId; // user_id
    private String email;   // email(ID)
    private String name;    // user name
    private String role;    // 권한(STUDENT, TUTOR, ADMIN 등)
}
