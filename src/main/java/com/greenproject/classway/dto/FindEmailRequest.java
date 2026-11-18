package com.greenproject.classway.dto;

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
public class FindEmailRequest {
    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "전화번호를 입력해주세요.")
    @Size(max = 20)
    private String phone;
}
