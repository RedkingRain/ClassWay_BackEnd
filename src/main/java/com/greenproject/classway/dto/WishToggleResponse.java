package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 찜 토글 응답 (CLASS-003): 화면이 하트 색과 숫자를 바로 바꿀 수 있게
@Getter
@Builder
@AllArgsConstructor
public class WishToggleResponse {
    private final Boolean wished;     // true = 방금 찜함, false = 방금 해제함
    private final Integer wishCount;  // 바뀐 뒤 찜 수
}