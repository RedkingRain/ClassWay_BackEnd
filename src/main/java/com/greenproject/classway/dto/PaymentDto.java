package com.greenproject.classway.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

public class PaymentDto {

    // [POST /payment/ready_order] 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadyRequest {
        @NotEmpty(message = "주문 상품 목록이 비어 있습니다.")
        private List<ItemRequest> items;

        private Integer userCouponId;
        private Integer usePoint = 0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        @NotNull(message = "클래스 ID는 필수입니다.")
        private Integer classId;

        private Integer scheduleId; // 오프라인인 경우 필수

        @NotNull(message = "수량은 필수입니다.")
        private Integer quantity;

        private Integer price;
        private Integer pointDiscount;
        private Integer couponDiscount;
        private Integer finalItemPrice;
    }

    // [POST /payment/ready_order] 응답 DTO (토스 결제창 v2 연동용)
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReadyResponse {
        private String orderNo;         // 토스 SDK orderId
        private String orderName;       // 토스 SDK orderName
        private Integer amount;          // 토스 SDK amount (최종 결제 금액)
        private String customerName;
        private String customerEmail;
    }

    // [POST /payment/orders_confirm] 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmRequest {
        @NotNull(message = "paymentKey는 필수입니다.")
        private String paymentKey;

        @NotNull(message = "order_no는 필수입니다.")
        private String order_no;

        @NotNull(message = "결제 금액은 필수입니다.")
        private Integer amount;
    }

    // [POST /payment/orders_confirm] 응답 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfirmResponse {
        private String orderNo;
        private String paymentKey;
        private Integer amount;
        private String status;          // "DONE"
        private String receiptUrl;      // 영수증 주소
        private String approvedAt;      // 승인 일시
    }

    // 토스페이먼츠 승인 API 응답 매핑 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TossApproveResponse {
        private String paymentKey;
        private String orderId;
        private String status;
        private String method;
        private Integer totalAmount;
        private String approvedAt;
        private TossReceipt receipt;
        private TossFailure failure;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class TossReceipt {
            private String url;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class TossFailure {
            private String code;
            private String message;
        }
    }

    // [POST /payment/cancel_order] 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelPendingRequest {
        @NotNull(message = "order_no는 필수입니다.")
        private String order_no;
    }
}
