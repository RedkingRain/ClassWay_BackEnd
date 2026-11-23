package com.greenproject.classway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 클래스 Q&A 문의 등록 요청 (POST /classes/{classId}/qnas) - CLASS-003
// 회원 번호는 받지 않음 (로그인 토큰에서 꺼냄)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassQnaCreateRequest {
    @NotBlank(message = "문의하실 내용을 작성해주세요.")
    @Size(max = 1000, message = "문의는 1000자까지 작성할 수 있습니다.")
    private String question;

    private Boolean isSecret; // 안 보내면 서버에서 false로 저장
}