package com.greenproject.classway.service;

import com.greenproject.classway.dto.CartItemResponse;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.repository.CartItemRepository;
import com.greenproject.classway.repository.CartItemRepository.CartItemRow;
import com.greenproject.classway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    /**
     * 장바구니 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CartItemResponse> getCartList() {

        // 1. 현재 접속자의 회원 정보 조회 (Spring Security Context 토큰 기반)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자를 찾을 수 없습니다: " + email));

        // 2. DB에서 장바구니 원본 데이터 목록 조회
        List<CartItemRow> rawItemList = cartItemRepository.findCartItemsByUserId(user.getId());

        // 3. UI에 표시할 응답 데이터 List 초기화
        List<CartItemResponse> cartResponseList = new ArrayList<>();

        // 4. 조회된 데이터를 1건씩 순회하며 비즈니스 로직 적용 및 DTO 조립
        for (CartItemRow row : rawItemList) {
            boolean isOnline = "ONLINE".equalsIgnoreCase(row.getClassType());

            // 수량 및 가격 계산
            int quantity = row.getQuantity() != null ? row.getQuantity() : 1;
            int salePrice = row.getSalePrice() != null ? row.getSalePrice() : row.getPrice();
            int itemTotalPrice = salePrice * quantity;

            // 정원 및 잔여 좌석 계산 (오프라인 전용)
            int capacity = row.getCapacity() != null ? row.getCapacity() : 0;
            int orderCapacity = row.getOrderCapacity() != null ? row.getOrderCapacity() : 0;
            int remainingCapacity = Math.max(0, capacity - orderCapacity);

            // [UI 비활성화 판별 로직]
            boolean isAvailable = true;
            String disabledReason = null;

            if (!"OPEN".equalsIgnoreCase(row.getClassStatus())) {
                isAvailable = false;
                disabledReason = "현재 수강 신청이 불가능한 클래스입니다.";
            } else if (!isOnline) {
                if (!"OPEN".equalsIgnoreCase(row.getScheduleStatus())) {
                    isAvailable = false;
                    disabledReason = "마감된 일정입니다. 회차를 다시 선택해 주세요.";
                } else if (remainingCapacity < quantity) {
                    isAvailable = false;
                    disabledReason = "정원이 마감되어 결제할 수 없습니다. 회차를 다시 선택해 주세요.";
                }
            }

            // 요청하신 항목 구성에 맞춰 DTO 생성
            CartItemResponse response = CartItemResponse.builder()
                    /* [히든 데이터] */
                    .cartItemId(row.getCartItemId())
                    .classId(row.getClassId())
                    .scheduleId(row.getScheduleId())

                    /* [클래스 목록 표시 데이터] */
                    .classType(isOnline ? "온라인" : "오프라인")
                    .rawClassType(row.getClassType())
                    .title(row.getTitle())
                    .thumbnailUrl(row.getThumbnailUrl())
                    .region(row.getRegion())
                    .vodCnt(row.getVodCnt())
                    .startAt(row.getStartAt())
                    .quantity(quantity)
                    .price(row.getPrice())
                    .salePrice(salePrice)
                    .saleRate(row.getSaleRate() != null ? row.getSaleRate() : 0)
                    .itemTotalPrice(itemTotalPrice)

                    /* [UI 결제 비활성화 제어 데이터] */
                    .classStatus(row.getClassStatus())
                    .scheduleStatus(row.getScheduleStatus())
                    .capacity(isOnline ? null : capacity)
                    .orderCapacity(isOnline ? null : orderCapacity)
                    .remainingCapacity(isOnline ? null : remainingCapacity)
                    .isAvailable(isAvailable)
                    .disabledReason(disabledReason)
                    .build();

            // 가공 완료된 한 건을 List에 추가
            cartResponseList.add(response);
        }

        // 5. 완성된 List 반환
        return cartResponseList;
    }
}
