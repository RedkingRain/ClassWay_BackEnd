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
import com.greenproject.classway.dto.CartAddRequest;
import com.greenproject.classway.dto.CartAddResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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


    @Operation(summary = "장바구니 담기 API", description = "클래스(오프라인은 일정 · 인원 포함)를 장바구니에 담습니다. 같은 클래스가 이미 있으면 담지 않고 alreadyInCart: true")
    @PostMapping
    public ResponseEntity<ApiResponse<CartAddResponse>> addToCart(
            @Valid @RequestBody CartAddRequest request
    ) {
        CartAddResponse result = cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
