package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    /* [1] 히든 데이터 (식별자) */
    private Integer cartItemId;          // 장바구니 item 고유번호
    private Integer classId;             // 클래스 고유번호
    private Integer scheduleId;          // 스케줄 고유번호 (오프라인의 경우)

    /* [2] 클래스 목록 표시 데이터 */
    private String classType;            // 화면 표시용 타입 ("온라인" / "오프라인")
    private String rawClassType;         // 원본 타입 ("ONLINE" / "OFFLINE")
    private String title;                // 클래스명
    private String thumbnailUrl;         // 클래스 썸네일 이미지 URL
    private String region;               // 온라인: "수강 기한 없음", 오프라인: "서울 성동구" 등
    private String vodCnt;               // 온라인: "VOD 30강", 오프라인: null
    private String startAt;              // 오프라인: "2026.11.08 (일) 14:00", 온라인: null
    private Integer quantity;            // 장바구니 담은 수량(신청 인원 수)
    private Integer price;               // 정가 (원)
    private Integer salePrice;           // 할인 판매가 (원)
    private Integer saleRate;            // 할인율 (%)
    private Integer itemTotalPrice;      // 아이템별 총 결제금액 (salePrice * quantity)

    /* [3] UI 결제 선택 비활성화를 위한 상태 데이터 */
    private String classStatus;          // 클래스 수강 가능 상태 (OPEN, CLOSED 등)
    private String scheduleStatus;       // 오프라인 스케줄 상태 (OPEN, CLOSED 등)
    private Integer capacity;            // 오프라인 스케줄 수강 정원
    private Integer orderCapacity;       // 현재 신청 인원 (결제 대기 + 결제 완료)
    private Integer remainingCapacity;   // 잔여 좌석 수 (capacity - orderCapacity)
    private Boolean isAvailable;         // 결제 선택(체크박스) 활성화 가능 여부
    private String disabledReason;       // 비활성화 사유 안내 문구 (정원 마감 등)
}
