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
}
