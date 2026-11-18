package com.greenproject.classway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 정보 수정 요청 DTO (MY-001)
 * - 프론트 -> 서버로 수정할 값을 받는 상자
 * - 이메일, 이름은 수정 불가라서 받지 않음.
 */
@NoArgsConstructor // Spring이 빈 객체를 먼저 만든 뒤 JSON 값을 채우기 때문에 필요
@Getter            // Service에서 값을 꺼낼 때 사용
public class MyProfileUpdateRequest {
    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해주세요.")
    private String nickname; // 변경할 닉네임

    @NotBlank(message = "연락처를 입력해주세요.")

    // 형식: 01X + 가운데 3~4자리 + 끝 4자리, 하이픈 있어도/없어도 됨
    // 가운데 3자리는 옛 번호(011-123-4567 등) 때문에 허용 → 0101234567도 형식상 통과
    @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "연락처는 010-1234-5678 또는 01012345678 형식으로 입력해주세요.")
    private String phone; // 변경할 연락처 (하이픈 있어도/없어도 됨, 저장은 숫자만)
}
