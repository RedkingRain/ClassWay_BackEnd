package com.greenproject.classway.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PasswordResetRequest {
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "인증번호를 입력해주세요.")
    private String code;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(max = 20)
    @JsonAlias({"newPassword", "new_password"})
    private String password;
}
