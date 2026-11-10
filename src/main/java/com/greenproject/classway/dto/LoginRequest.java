package com.greenproject.classway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginRequest {
    @NotBlank       // Null or 빈 문자열 방지 규칙 추가(공백으로 이루어진 데이터도 빈 값으로 간주)
    @Email
    private String email;
    @NotBlank
    private String password;
}
