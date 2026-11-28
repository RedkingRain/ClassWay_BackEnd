package com.greenproject.classway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    // [POST /payment/ready_order] 응답 DTO (토스 결제창 연동용)
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

    // [POST /payment/orders_confirm] 및 [GET /payment/orders/{order_no}/status] 공통 응답 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfirmResponse {
        private String orderNo;
        private String orderName;
        private Integer amount;
        // status: COMPLETED, RESERVATION_FAILED, PAYMENT_FAILED, PROCESSING, EXPIRED
        private String status;
        private String paymentMethod;     // 예: "신용카드 (국민 ****1234)"
        private String reservationStatus; // 예: "예약 확정"
        private String receiptUrl;        // 영수증 주소
        private String approvedAt;        // 승인 일시
        private String cancelStatus;      // 취소 상태 ("취소 완료")
        private String message;           // 안내 메시지
        private String errorCode;         // 에러 코드
        private String errorMessage;       // 에러 상세 메시지
        private String paymentStatus;     // "처리 중"
    }

    // 토스페이먼츠 승인 및 단건 조회 응답 매핑 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TossApproveResponse {
        private String paymentKey;
        private String orderId;
        private String status;
        private String method;
        private Integer totalAmount;
        private String approvedAt;
        private TossReceipt receipt;
        private TossCard card;
        private TossEasyPay easyPay;
        private TossFailure failure;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TossReceipt {
            private String url;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TossCard {
            private String issuerCode;
            private String number;
            private String cardType;
            private String ownerType;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TossEasyPay {
            private String provider;
            private Integer amount;
            private Integer discountAmount;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TossFailure {
            private String code;
            private String message;
        }
    }

    // 토스페이먼츠 결제 취소 API 응답 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TossCancelResponse {
        private String paymentKey;
        private String status;
        private String orderId;
        private List<TossCancelDetail> cancels;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class TossCancelDetail {
            private Integer cancelAmount;
            private String cancelReason;
            private String canceledAt;
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

    // [POST /payment/refund_order] 요청 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequest {
        @NotNull(message = "order_no는 필수입니다.")
        private String order_no;

        // 특정 강의 1개만 골라 부분 취소할 때 전달 (생략 시 전체 또는 order_item_ids 기준)
        private Integer order_item_id;

        // 여러 강의 중 특정 강의들만 골라 부분 취소할 때 전달 (order_item_id, order_item_ids 모두 생략 시 주문 내 결제완료 강의 전체 취소)
        private List<Integer> order_item_ids;

        private String reason = "고객 요청에 의한 결제 취소";

        public RefundRequest(String order_no, String reason) {
            this.order_no = order_no;
            this.reason = reason;
        }
    }

    // [POST /payment/refund_order] 응답 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RefundResponse {
        private String orderNo;
        private List<Integer> refundedOrderItemIds; // 이번에 환불 처리된 order_item_id 목록
        private Integer refundAmount;
        private Integer refundRate; // 100 또는 50
        private String status;      // "REFUNDED" 또는 "PARTIAL_REFUNDED"
        private String message;
    }
}
