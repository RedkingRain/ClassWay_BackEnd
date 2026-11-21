package com.greenproject.classway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 페이지 강사 신청 반려 요청 (PATCH /admin/tutors/{tutorId}/reject) -(ADMIN-002)
 * - 반려 사유 필수
 * - 길이 제한은 DB tutors.reject_reason 칸 크기(255)와 맞춤
 */
@NoArgsConstructor
@Getter
@Setter // JSON 요청 값을 필드에 넣을 때 사용 (다른 요청 DTO와 바인딩 방식 통일)
public class TutorRejectRequest {
    @NotBlank(message = "반려 사유를 입력해주세요.") //null, "", "   " 모두 막음
    @Size(max = 255, message = "반려 사유는 255자 이내로 입력해주세요.") //DB 칸보다 길면 저장 에러
    private String rejectReason;
}
