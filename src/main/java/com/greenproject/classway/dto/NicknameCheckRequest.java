package com.greenproject.classway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NicknameCheckRequest {
    @NotBlank(message = "닉네임을 입력해주세요.")
    private String nickname;
}
