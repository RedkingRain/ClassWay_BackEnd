package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.PaymentDto.*;
import com.greenproject.classway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment API", description = "결제 준비 및 토스페이먼츠 승인 API 모듈입니다.")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "클래스 결제 가능 상태 조회 및 주문 대기 생성 API",
               description = "결제 전 클래스 상태/잔여 정원을 검증하고 PENDING 주문을 생성하여 토스 결제용 order_no를 반환합니다.")
    @PostMapping("/ready_order")
    public ResponseEntity<ApiResponse<ReadyResponse>> readyOrder(
            @Valid @RequestBody ReadyRequest request
    ) {
        ReadyResponse response = paymentService.readyOrder(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "토스 결제 최종 승인 API",
               description = "토스 결제창 완료 후 전달받은 paymentKey와 amount를 검증하고 토스 승인 API를 호출합니다.")
    @PostMapping("/orders_confirm")
    public ResponseEntity<ApiResponse<ConfirmResponse>> confirmOrder(
            @Valid @RequestBody ConfirmRequest request
    ) {
        ConfirmResponse response = paymentService.confirmOrder(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "결제창 취소 및 대기 주문 정리 API",
               description = "사용자가 토스 결제창을 닫거나 취소했을 때 PENDING/READY 상태의 임시 주문 및 결제 데이터를 정리합니다.")
    @PostMapping("/cancel_order")
    public ResponseEntity<ApiResponse<Void>> cancelPendingOrder(
            @Valid @RequestBody CancelPendingRequest request
    ) {
        paymentService.cancelPendingOrder(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
