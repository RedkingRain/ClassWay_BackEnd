package com.greenproject.classway.service;

import com.greenproject.classway.dto.PaymentDto.*;
import com.greenproject.classway.entity.*;
import com.greenproject.classway.exceptions.InvalidStateException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.*;
import com.greenproject.classway.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final CartItemRepository cartItemRepository;

    @Value("${spring.toss.secret:test_sk_default}")
    private String tossSecretKey;

    // 클래스 상태/정원 체크 및 PENDING 주문/READY 결제 데이터 생성
    @Transactional
    public ReadyResponse readyOrder(ReadyRequest request) {

        // 1. 현재 접속자 확인 (JWT 토큰 기반)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // [알고리즘 1단계] PENDING 주문 및 READY 결제 데이터 삭제 (순수 JPA 활용, FK 순서 준수)
        List<Order> existingPendingOrders = orderRepository.findByUserAndStatus(user, OrderStatus.PENDING);
        if (!existingPendingOrders.isEmpty()) {
            // 1) 자식: payments (READY 상태) 삭제
            paymentRepository.deleteByOrderInAndStatus(existingPendingOrders, PaymentStatus.READY);
            // 2) 자식: order_items 삭제
            orderItemRepository.deleteByOrderIn(existingPendingOrders);
            // 3) 부모: orders 삭제
            orderRepository.deleteAll(existingPendingOrders);
        }

        // [알고리즘 2단계 & 3단계] 클래스 상태 및 스케줄 정원 검증
        int calculatedTotalAmount = 0;
        List<OrderItem> orderItemsToSave = new ArrayList<>();
        String firstClassTitle = null;

        // TSID 기반 주문번호 발급 (ORD-YYYYMMDD-TSID)
        String orderNo = TsidUtil.generateOrderNo();

        // Order 엔티티 준비
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        for (ItemRequest itemReq : request.getItems()) {
            ClassEntity clazz = classEntityRepository.findById(itemReq.getClassId())
                    .orElseThrow(() -> new InvalidStateException("구매할 수 없는 클래스가 존재합니다."));

            // 3-1: 클래스 오픈 상태(PUBLISHED) 검증
            if (clazz.getStatus() != ClassStatus.PUBLISHED) {
                throw new InvalidStateException("구매할 수 없는 클래스가 존재합니다.");
            }

            if (firstClassTitle == null) {
                firstClassTitle = clazz.getTitle();
            }

            ClassSchedule schedule = null;

            // 오프라인 클래스인 경우 정원 및 상태 체크
            if (clazz.getClassType() == ClassType.OFFLINE) {
                if (itemReq.getScheduleId() == null) {
                    throw new InvalidStateException("오프라인 클래스는 일정을 선택해야 합니다.");
                }

                schedule = classScheduleRepository.findById(itemReq.getScheduleId())
                        .orElseThrow(() -> new InvalidStateException("구매할 수 없는 클래스가 존재합니다."));

                // 3-3: 스케줄 상태 체크
                if (schedule.getStatus() != ScheduleStatus.OPEN) {
                    throw new InvalidStateException("구매할 수 없는 클래스가 존재합니다.");
                }

                // 3-2: 정원 마감 및 인원 수용 가능 여부 검사
                int reservedCount = orderItemRepository.countReservedCapacity(schedule.getScheduleId());
                int remaining = schedule.getCapacity() - reservedCount;

                if (remaining <= 0 || remaining < itemReq.getQuantity()) {
                    throw new InvalidStateException("이미 마감된 클래스가 있습니다.");
                }
            }

            // 금액 계산 (서버 재계산으로 위변조 방어)
            int itemPrice = (clazz.getSalePrice() != null) ? clazz.getSalePrice() : clazz.getPrice();
            int itemTotalPrice = itemPrice * itemReq.getQuantity();
            calculatedTotalAmount += itemTotalPrice;

            // OrderItem 생성
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setClazz(clazz);
            orderItem.setSchedule(schedule);
            orderItem.setClassTitle(clazz.getTitle());
            orderItem.setPrice(itemPrice);
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setFinalItemPrice(itemTotalPrice);
            orderItem.setStatus(OrderItemStatus.PENDING);

            orderItemsToSave.add(orderItem);
        }

        int finalAmount = Math.max(0, calculatedTotalAmount - (request.getUsePoint() != null ? request.getUsePoint() : 0));

        order.setTotalAmount(calculatedTotalAmount);
        order.setCouponDiscount(0);
        order.setPointUsed(request.getUsePoint() != null ? request.getUsePoint() : 0);
        order.setFinalAmount(finalAmount);

        // 3단계 저장: orders & order_items
        orderRepository.save(order);
        orderItemRepository.saveAll(orderItemsToSave);

        // 3단계 저장: payments (필수 항목만 입력, status = READY)
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(finalAmount);
        payment.setStatus(PaymentStatus.READY);
        paymentRepository.save(payment);

        // 4단계: 토스 연동용 데이터 반환
        int extraCount = request.getItems().size() - 1;
        String orderName = (extraCount > 0) ? firstClassTitle + " 외 " + extraCount + "건" : firstClassTitle;

        return ReadyResponse.builder()
                .orderNo(orderNo)
                .orderName(orderName)
                .amount(finalAmount)
                .customerName(user.getName())
                .customerEmail(user.getEmail())
                .build();
    }

    // 토스페이먼츠 결제 승인 요청 및 위·변조 검증
    // 승인 실패 시에도 FAILED/ABORTED 상태를 DB에 저장하기 위해 noRollbackFor 설정
    @Transactional(noRollbackFor = InvalidStateException.class)
    public ConfirmResponse confirmOrder(ConfirmRequest request) {

        // 1. 주문 조회 및 사용자 본인 검증
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderRepository.findByOrderNo(request.getOrder_no())
                .orElseThrow(() -> new InvalidStateException("존재하지 않는 주문입니다."));

        if (!order.getUser().getEmail().equals(email)) {
            throw new InvalidStateException("본인의 주문만 결제할 수 있습니다.");
        }

        // 2. 금액 대조 및 위변조 검증
        if (!order.getFinalAmount().equals(request.getAmount())) {
            throw new InvalidStateException("결제 진행 정보가 일치하지 않습니다. 다시 시도해주세요.");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new InvalidStateException("결제 대기 정보가 존재하지 않습니다."));

        // 3. 토스페이먼츠 결제 승인 API 호출
        // Authorization Header: Basic Base64(WIDGET_SECRET_KEY + ":")
        String authValue = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        RestClient restClient = RestClient.create();

        try {
            TossApproveResponse tossResponse = restClient.post()
                    .uri("https://api.tosspayments.com/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + authValue)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", request.getPaymentKey(),
                            "orderId", request.getOrder_no(),
                            "amount", request.getAmount()
                    ))
                    .retrieve()
                    .body(TossApproveResponse.class);

            // 4. 승인 성공: orders, order_items 상태값 'PAID'로 변경 후 저장
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
            for (OrderItem item : orderItems) {
                item.setStatus(OrderItemStatus.PAID);
            }
            orderItemRepository.saveAll(orderItems);

            // 5. payments 상태값 'DONE', failReason = null, 나머지 항목 업데이트 후 저장
            LocalDateTime approvedAt = (tossResponse != null && tossResponse.getApprovedAt() != null)
                    ? OffsetDateTime.parse(tossResponse.getApprovedAt()).toLocalDateTime()
                    : LocalDateTime.now();

            payment.setPaymentKey(request.getPaymentKey());
            payment.setMethod(tossResponse != null ? tossResponse.getMethod() : "CARD");
            payment.setStatus(PaymentStatus.DONE);
            payment.setFailReason(null);
            payment.setApprovedAt(approvedAt);
            if (tossResponse != null && tossResponse.getReceipt() != null) {
                payment.setReceiptUrl(tossResponse.getReceipt().getUrl());
            }
            paymentRepository.save(payment);

            // 6. reservations 테이블 데이터 추가 & 오프라인 강의 시 class_schedules의 reserved_count 증가
            List<Reservation> reservationsToSave = new ArrayList<>();
            List<ClassEntity> purchasedClasses = new ArrayList<>();

            for (OrderItem item : orderItems) {
                // 6-1) Reservation 엔티티 생성
                Reservation reservation = new Reservation();
                reservation.setUser(order.getUser());
                reservation.setClazz(item.getClazz());
                reservation.setSchedule(item.getSchedule());
                reservation.setOrderItem(item);
                reservation.setStatus(ReservationStatus.RESERVED);
                reservationsToSave.add(reservation);

                purchasedClasses.add(item.getClazz());

                // 6-2) 오프라인 강의일 경우 class_schedules의 reserved_count에 신청 인원 합산 후 저장
                ClassSchedule schedule = item.getSchedule();
                if (schedule != null) {
                    int currentReserved = schedule.getReservedCount() != null ? schedule.getReservedCount() : 0;
                    int updatedReserved = currentReserved + item.getQuantity();
                    schedule.setReservedCount(updatedReserved);

                    // 정원이 모두 찼다면 스케줄 상태를 FULL(마감)로 변경
                    if (updatedReserved >= schedule.getCapacity()) {
                        schedule.setStatus(ScheduleStatus.FULL);
                    }
                    classScheduleRepository.save(schedule);
                }
            }
            reservationRepository.saveAll(reservationsToSave);

            // 7. 장바구니에서 결제 완료된 항목 제거
            if (!purchasedClasses.isEmpty()) {
                cartItemRepository.deleteByUserAndClazzIn(order.getUser(), purchasedClasses);
            }

            // 8. 프론트엔드로 성공 결과 반환
            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .paymentKey(request.getPaymentKey())
                    .amount(order.getFinalAmount())
                    .status("DONE")
                    .receiptUrl(payment.getReceiptUrl())
                    .approvedAt(approvedAt.toString())
                    .build();

        } catch (Exception e) {
            // 승인 실패: orders, order_items 상태값 'FAILED'로 변경 후 저장
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
            for (OrderItem item : orderItems) {
                item.setStatus(OrderItemStatus.FAILED);
            }
            orderItemRepository.saveAll(orderItems);

            // payments 상태값 'ABORTED', 실패 사유 입력 (255자 초과 방어) 후 저장
            String rawReason = e.getMessage() != null ? e.getMessage() : "결제 승인 실패";
            String safeReason = rawReason.length() > 250 ? rawReason.substring(0, 250) : rawReason;

            payment.setPaymentKey(request.getPaymentKey());
            payment.setStatus(PaymentStatus.ABORTED);
            payment.setFailReason(safeReason);
            paymentRepository.save(payment);

            throw new InvalidStateException("토스페이먼츠 결제 승인에 실패하였습니다: " + safeReason);
        }
    }

    // 사용자가 결제창을 닫거나 취소했을 때 PENDING/READY 상태의 임시 주문 데이터 즉시 정리
    @Transactional
    public void cancelPendingOrder(CancelPendingRequest request) {
        // 1. 현재 접속자 확인 (JWT)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. 주문 조회
        Order order = orderRepository.findByOrderNo(request.getOrder_no())
                .orElseThrow(() -> new ResourceNotFoundException("해당 주문을 찾을 수 없습니다: " + request.getOrder_no()));

        // 3. 본인 확인
        if (!order.getUser().getEmail().equals(email)) {
            throw new InvalidStateException("본인의 주문만 취소할 수 있습니다.");
        }

        // 4. 안전장치: 이미 결제 완료(PAID)된 건은 절대 삭제되지 않도록 PENDING 상태만 삭제 허용
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidStateException("결제 대기(PENDING) 상태의 주문만 취소할 수 있습니다.");
        }

        // 5. 순수 JPA를 활용한 자식 -> 부모 순차 삭제 (FK 무결성 준수)
        List<Order> targetOrders = List.of(order);
        // 1) Payment 삭제 (status = READY)
        paymentRepository.deleteByOrderInAndStatus(targetOrders, PaymentStatus.READY);
        // 2) OrderItem 삭제
        orderItemRepository.deleteByOrderIn(targetOrders);
        // 3) Order 삭제
        orderRepository.delete(order);
    }
}
