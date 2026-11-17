package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 장바구니 담기 응답 (POST /cart)
@Getter
@Builder
@AllArgsConstructor
public class CartAddResponse {
    private final Integer cartItemId;     // 담긴(또는 이미 있던) 장바구니 번호
    private final Boolean alreadyInCart;  // 이미 담겨 있어서 새로 담지 않았는지 → 프론트는 장바구니로 이동
}