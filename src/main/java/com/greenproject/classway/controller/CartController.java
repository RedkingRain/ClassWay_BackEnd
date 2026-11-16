package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.CartItemResponse;
import com.greenproject.classway.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Cart API", description = "장바구니 관련 API 모듈입니다.")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "장바구니 목록 조회 API", description = "로그인된 회원의 장바구니 목록과 UI 렌더링에 필요한 상세 데이터(일정, 정원 상태 등)를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCart() {
        List<CartItemResponse> result = cartService.getCartList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
