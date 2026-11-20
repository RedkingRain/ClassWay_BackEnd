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
import com.greenproject.classway.dto.CartAddRequest;
import com.greenproject.classway.dto.CartAddResponse;
import com.greenproject.classway.dto.CartDeleteRequest;
import com.greenproject.classway.entity.CartItem;
import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.entity.ClassSchedule;
import com.greenproject.classway.exceptions.InvalidStateException;
import com.greenproject.classway.exceptions.ResourceConflictException;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import com.greenproject.classway.global.ReservationStatus;
import com.greenproject.classway.global.ScheduleStatus;
import com.greenproject.classway.repository.ClassEntityRepository;
import com.greenproject.classway.repository.ClassScheduleRepository;
import com.greenproject.classway.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ReservationRepository reservationRepository;

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

            if (!"PUBLISHED".equalsIgnoreCase(row.getClassStatus())) {
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


    /**
     * 장바구니 담기 (CLASS-002), 가격은 요청으로 받지 않고 결제 때 서버가 DB 가격으로 계산함
     */
    @Transactional
    public CartAddResponse addToCart(CartAddRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자를 찾을 수 없습니다."));

        Integer classId = request.getClassId();
        ClassEntity clazz = classEntityRepository.findByClassIdAndStatus(classId, ClassStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 강의입니다."));
        boolean isOnline = clazz.getClassType() == ClassType.ONLINE;

        // 온라인 클래스를 두 번 결제하지 않도록 이미 수강 중이면 거절함 (상세의 isEnrolled와 같은 기준)
        if (isOnline && reservationRepository.existsByUser_EmailAndClazz_ClassIdAndStatus(
                email, classId, ReservationStatus.RESERVED)) {
            throw new ResourceConflictException("이미 수강 중인 클래스입니다.");
        }

        // 같은 클래스가 장바구니에 두 번 들어가지 않게, 이미 담겨 있으면 새로 담지 않고 이미 있다고 알려줌
        Optional<CartItem> existing = cartItemRepository.findFirstByUser_IdAndClazz_ClassId(user.getId(), classId);
        if (existing.isPresent()) {
            return CartAddResponse.builder()
                    .cartItemId(existing.get().getCartItemId())
                    .alreadyInCart(true)
                    .build();
        }

        // 온라인은 일정이 없고 인원은 1명으로 고정함
        ClassSchedule schedule = null;
        int quantity = 1;
        if (!isOnline) {
            if (request.getScheduleId() == null) {
                throw new InvalidStateException("날짜와 시간을 선택해 주세요.");
            }

            // 다른 클래스의 일정 번호를 끼워 넣어도 없는 일정으로 처리함
            schedule = classScheduleRepository.findById(request.getScheduleId())
                    .filter((s) -> s.getClazz().getClassId().equals(classId))
                    .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 일정입니다."));

            if (schedule.getStatus() != ScheduleStatus.OPEN
                    || !schedule.getStartAt().isAfter(LocalDateTime.now())) {
                throw new InvalidStateException("마감된 일정입니다.");
            }

            quantity = request.getQuantity();
            int remainingSeats = schedule.getCapacity() - schedule.getReservedCount();
            if (quantity > remainingSeats) {
                throw new InvalidStateException("남은 자리보다 인원이 많습니다.");
            }
        }

        CartItem cartItem = new CartItem();
        cartItem.setUser(user);
        cartItem.setClazz(clazz);
        cartItem.setSchedule(schedule);
        cartItem.setQuantity(quantity);
        CartItem saved = cartItemRepository.save(cartItem);

        return CartAddResponse.builder()
                .cartItemId(saved.getCartItemId())
                .alreadyInCart(false)
                .build();
    }

    /**
     * 장바구니 선택(및 개별) 항목 삭제
     */
    @Transactional
    public void deleteCartItems(CartDeleteRequest request) {
        // 1. 현재 접속자 확인 (JWT 토큰 기반)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자를 찾을 수 없습니다."));

        // 2. 본인 소유의 장바구니 항목들만 일괄 삭제
        cartItemRepository.deleteByUserAndCartItemIdIn(user, request.getCartItemIds());
    }
}
