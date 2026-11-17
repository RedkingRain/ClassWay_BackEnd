package com.greenproject.classway.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 장바구니 담기 요청 (POST /cart) — CLASS-002
// 회원 번호 · 가격은 받지 않음 (회원은 로그인 토큰으로, 가격은 결제 때 서버가 DB로 계산)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CartAddRequest {
    @NotNull
    private Integer classId;

    private Integer scheduleId;   // 오프라인만 (온라인은 null)

    @NotNull
    @Min(1)
    private Integer quantity;     // 인원 (온라인은 서버에서 1로 고정)
}