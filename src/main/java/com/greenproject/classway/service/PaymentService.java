package com.greenproject.classway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenproject.classway.dto.PaymentDto.*;
import com.greenproject.classway.entity.*;
import com.greenproject.classway.exceptions.InvalidStateException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.*;
import com.greenproject.classway.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
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
    private final RefundRepository refundRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    @Value("${spring.toss.secret:test_sk_default}")
    private String tossSecretKey;

    // 타임아웃이 설정된 RestClient (Connect 3s, Read 12s)
    protected RestClient createTimeoutRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(12));
        return RestClient.builder().requestFactory(factory).build();
    }

    // 내부 호출(Self-Invocation) 시에도 실제 독립 트랜잭션(REQUIRES_NEW)을 보장하는 헬퍼
    private <T> T executeInNewTransaction(Supplier<T> action) {
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return template.execute(status -> action.get());
        }
        return action.get();
    }

    private void runInNewTransaction(Runnable action) {
        executeInNewTransaction(() -> {
            action.run();
            return null;
        });
    }

    // 결제 준비 및 10분 유효 대기 주문 생성
    @Transactional
    public ReadyResponse readyOrder(ReadyRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // 이전 PENDING 주문 정리 (단, IN_PROGRESS 토스 지연 건은 삭제 제외)
        List<Order> existingPendingOrders = orderRepository.findByUserAndStatus(user, OrderStatus.PENDING);
        List<Order> ordersToDelete = new ArrayList<>();
        for (Order o : existingPendingOrders) {
            Payment p = paymentRepository.findByOrder(o).orElse(null);
            if (p != null && p.getStatus() == PaymentStatus.IN_PROGRESS) {
                continue; // 보존
            }
            ordersToDelete.add(o);
        }

        if (!ordersToDelete.isEmpty()) {
            paymentRepository.deleteByOrderInAndStatus(ordersToDelete, PaymentStatus.READY);
            orderItemRepository.deleteByOrderIn(ordersToDelete);
            orderRepository.deleteAll(ordersToDelete);
        }

        int calculatedTotalAmount = 0;
        List<OrderItem> orderItemsToSave = new ArrayList<>();
        String firstClassTitle = null;

        String orderNo = TsidUtil.generateOrderNo();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // 10분 만료 시간 부여

        for (ItemRequest itemReq : request.getItems()) {
            ClassEntity clazz = classEntityRepository.findById(itemReq.getClassId())
                    .orElseThrow(() -> new InvalidStateException("구매할 수 없는 클래스가 존재합니다."));

            if (clazz.getStatus() != ClassStatus.PUBLISHED) {
                throw new InvalidStateException("구매할 수 없는 클래스가 존재합니다.");
            }

            if (firstClassTitle == null) {
                firstClassTitle = clazz.getTitle();
            }

            ClassSchedule schedule = null;
            if (clazz.getClassType() == ClassType.OFFLINE) {
                if (itemReq.getScheduleId() == null) {
                    throw new InvalidStateException("오프라인 클래스는 일정을 선택해야 합니다.");
                }

                schedule = classScheduleRepository.findById(itemReq.getScheduleId())
                        .orElseThrow(() -> new InvalidStateException("구매할 수 없는 클래스가 존재합니다."));

                if (schedule.getStatus() != ScheduleStatus.OPEN) {
                    throw new InvalidStateException("구매할 수 없는 클래스가 존재합니다.");
                }

                int reservedCount = orderItemRepository.countReservedCapacity(schedule.getScheduleId());
                int remaining = schedule.getCapacity() - reservedCount;

                if (remaining <= 0 || remaining < itemReq.getQuantity()) {
                    throw new InvalidStateException("이미 마감된 클래스가 있습니다.");
                }
            }

            int itemPrice = (clazz.getSalePrice() != null) ? clazz.getSalePrice() : clazz.getPrice();
            int itemTotalPrice = itemPrice * itemReq.getQuantity();
            calculatedTotalAmount += itemTotalPrice;

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

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItemsToSave);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(finalAmount);
        payment.setStatus(PaymentStatus.READY);
        paymentRepository.save(payment);

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

    // 최종 결제 승인 API (5가지 케이스 분기)
    public ConfirmResponse confirmOrder(ConfirmRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderRepository.findByOrderNo(request.getOrder_no())
                .orElseThrow(() -> new InvalidStateException("존재하지 않는 주문입니다."));

        if (!order.getUser().getEmail().equals(email)) {
            throw new InvalidStateException("본인의 주문만 결제할 수 있습니다.");
        }

        if (!order.getFinalAmount().equals(request.getAmount())) {
            throw new InvalidStateException("결제 진행 정보가 일치하지 않습니다. 다시 시도해주세요.");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new InvalidStateException("결제 대기 정보가 존재하지 않습니다."));

        // [멱등성 방어] 이미 결제 완료(PAID)된 주문으로 중복 승인 요청(새로고침 등)이 들어온 경우 즉시 성공 응답 반환
        if (order.getStatus() == OrderStatus.PAID && payment.getStatus() == PaymentStatus.DONE) {
            List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
            return buildCompletedResponseFromDb(order, payment, orderItems);
        }

        // [멱등성 방어] 이미 정원 초과로 자동 취소된 주문인 경우
        if (payment.getStatus() == PaymentStatus.CANCELED && "RESERVATION_FAILED".equals(payment.getFailReason())) {
            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getFinalAmount())
                    .status("RESERVATION_FAILED")
                    .cancelStatus("취소 완료")
                    .message("결제는 승인되었지만 정원 초과로 예약을 확정하지 못했습니다. 결제 금액은 자동으로 취소되었습니다.")
                    .build();
        }

        // [사전 검증] 10분 유효시간 만료 여부 확인
        if (LocalDateTime.now().isAfter(order.getExpiresAt()) || order.getStatus() == OrderStatus.EXPIRED) {
            handleOrderExpired(order, payment);
            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getFinalAmount())
                    .status("EXPIRED")
                    .errorCode("ORDER_EXPIRED")
                    .errorMessage("결제 유효시간(10분)이 만료되었습니다. 다시 시도해 주세요.")
                    .build();
        }

        String authValue = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        RestClient restClient = createTimeoutRestClient();

        try {
            // 토스페이먼츠 결제 승인 API 호출
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

            // CASE 1: 승인 성공 -> DB 예약 확정 처리 (독립 트랜잭션 보장)
            return completeReservationInTx(order, payment, request.getPaymentKey(), tossResponse);

        } catch (HttpStatusCodeException e) {
            // CASE 2: 토스 승인 거절 (4xx/5xx)
            String responseBody = e.getResponseBodyAsString();
            String errorCode = "PAYMENT_FAILED";
            String errorMessage = "결제 승인이 거절되었습니다.";

            try {
                TossApproveResponse errResp = objectMapper.readValue(responseBody, TossApproveResponse.class);
                if (errResp.getFailure() != null) {
                    errorCode = errResp.getFailure().getCode();
                    errorMessage = errResp.getFailure().getMessage();
                }
            } catch (Exception ignored) {}

            if ("EXCEED_MAX_AUTH_TIME".equalsIgnoreCase(errorCode)) {
                handleOrderExpired(order, payment);
                return ConfirmResponse.builder()
                        .orderNo(order.getOrderNo())
                        .amount(order.getFinalAmount())
                        .status("EXPIRED")
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .build();
            }

            handlePaymentFailed(order, payment, request.getPaymentKey(), errorMessage);
            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getFinalAmount())
                    .status("PAYMENT_FAILED")
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .build();

        } catch (ResourceAccessException e) {
            // CASE 3: 토스 응답 지연 (타임아웃) -> 단건 조회 시도
            log.warn("[PaymentConfirm] 토스 승인 타임아웃 발생 -> 단건 조회 1회 시도: orderNo={}", order.getOrderNo());
            TossApproveResponse checkResponse = fetchTossPaymentSingle(order.getOrderNo());

            if (checkResponse != null && "DONE".equalsIgnoreCase(checkResponse.getStatus())) {
                return completeReservationInTx(order, payment, request.getPaymentKey(), checkResponse);
            }

            // 조회도 불가한 경우: IN_PROGRESS 유지
            handlePaymentInProgress(payment, request.getPaymentKey());
            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getFinalAmount())
                    .status("PROCESSING")
                    .paymentStatus("처리 중")
                    .message("결제가 처리 중입니다. 잠시 후 상태를 다시 확인해 주세요.")
                    .build();
        }
    }

    // [독립 트랜잭션] DB 예약 확정 및 정원 초과 시 토스 자동 롤백
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmResponse completeReservationInTx(Order order, Payment payment, String paymentKey, TossApproveResponse tossResponse) {
        return executeInNewTransaction(() -> {
            List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
            Map<Integer, ClassSchedule> lockedSchedules = new HashMap<>();

            // 1. 오프라인 클래스 정원 비관적 락 재검증
            for (OrderItem item : orderItems) {
                if (item.getSchedule() != null) {
                    Integer scheduleId = item.getSchedule().getScheduleId();
                    ClassSchedule schedule = classScheduleRepository.findByIdWithLock(scheduleId)
                            .orElseThrow(() -> new InvalidStateException("스케줄 정보를 찾을 수 없습니다."));
                    lockedSchedules.put(scheduleId, schedule);

                    int currentReserved = schedule.getReservedCount() != null ? schedule.getReservedCount() : 0;
                    if (currentReserved + item.getQuantity() > schedule.getCapacity()) {
                        // 정원 초과! 즉시 토스 자동 취소 호출
                        log.error("[PaymentConfirm] 정원 초과 감지 -> 자동 결제 취소 실행: orderNo={}", order.getOrderNo());
                        cancelTossPayment(paymentKey, "정원 초과로 인한 예약 확정 실패 자동 환불", null);

                        order.setStatus(OrderStatus.CANCELED);
                        orderRepository.save(order);

                        for (OrderItem oi : orderItems) {
                            oi.setStatus(OrderItemStatus.CANCELED);
                        }
                        orderItemRepository.saveAll(orderItems);

                        payment.setPaymentKey(paymentKey);
                        payment.setStatus(PaymentStatus.CANCELED);
                        payment.setFailReason("RESERVATION_FAILED");
                        paymentRepository.save(payment);

                        return ConfirmResponse.builder()
                                .orderNo(order.getOrderNo())
                                .amount(order.getFinalAmount())
                                .status("RESERVATION_FAILED")
                                .cancelStatus("취소 완료")
                                .message("결제는 승인되었지만 정원 초과로 예약을 확정하지 못했습니다. 결제 금액은 자동으로 취소되었습니다.")
                                .build();
                    }
                }
            }

            // 2. 예약 확정 성공 처리
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            for (OrderItem item : orderItems) {
                item.setStatus(OrderItemStatus.PAID);
            }
            orderItemRepository.saveAll(orderItems);

            LocalDateTime approvedAt = (tossResponse != null && tossResponse.getApprovedAt() != null)
                    ? OffsetDateTime.parse(tossResponse.getApprovedAt()).toLocalDateTime()
                    : LocalDateTime.now();

            payment.setPaymentKey(paymentKey);
            payment.setMethod(tossResponse != null ? tossResponse.getMethod() : "CARD");
            payment.setStatus(PaymentStatus.DONE);
            payment.setFailReason(null);
            payment.setApprovedAt(approvedAt);
            if (tossResponse != null && tossResponse.getReceipt() != null) {
                payment.setReceiptUrl(tossResponse.getReceipt().getUrl());
            }
            paymentRepository.save(payment);

            List<Reservation> reservationsToSave = new ArrayList<>();
            List<ClassEntity> purchasedClasses = new ArrayList<>();

            for (OrderItem item : orderItems) {
                Reservation reservation = new Reservation();
                reservation.setUser(order.getUser());
                reservation.setClazz(item.getClazz());
                reservation.setSchedule(item.getSchedule());
                reservation.setOrderItem(item);
                reservation.setStatus(ReservationStatus.RESERVED);
                reservationsToSave.add(reservation);

                purchasedClasses.add(item.getClazz());

                if (item.getSchedule() != null) {
                    ClassSchedule schedule = lockedSchedules.getOrDefault(
                            item.getSchedule().getScheduleId(), item.getSchedule());
                    int currentReserved = schedule.getReservedCount() != null ? schedule.getReservedCount() : 0;
                    int updatedReserved = currentReserved + item.getQuantity();
                    schedule.setReservedCount(updatedReserved);
                    if (updatedReserved >= schedule.getCapacity()) {
                        schedule.setStatus(ScheduleStatus.FULL);
                    }
                    classScheduleRepository.save(schedule);
                }
            }
            reservationRepository.saveAll(reservationsToSave);

            if (!purchasedClasses.isEmpty()) {
                cartItemRepository.deleteByUserAndClazzIn(order.getUser(), purchasedClasses);
            }

            String orderName = buildOrderName(orderItems);

            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .orderName(orderName)
                    .amount(order.getFinalAmount())
                    .status("COMPLETED")
                    .paymentMethod(formatPaymentMethod(tossResponse))
                    .reservationStatus("예약 확정")
                    .receiptUrl(payment.getReceiptUrl())
                    .approvedAt(approvedAt.toString())
                    .build();
        });
    }

    // 결제 상태 재확인 API
    public ConfirmResponse checkOrderStatus(String orderNo) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("해당 주문을 찾을 수 없습니다: " + orderNo));

        if (!order.getUser().getEmail().equals(email)) {
            throw new InvalidStateException("본인의 주문만 조회할 수 있습니다.");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new InvalidStateException("결제 정보를 찾을 수 없습니다."));

        // Fast-Path (이미 확정된 경우)
        if (order.getStatus() == OrderStatus.PAID && payment.getStatus() == PaymentStatus.DONE) {
            List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
            return buildCompletedResponseFromDb(order, payment, orderItems);
        }

        if (payment.getStatus() == PaymentStatus.CANCELED && "RESERVATION_FAILED".equals(payment.getFailReason())) {
            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getFinalAmount())
                    .status("RESERVATION_FAILED")
                    .cancelStatus("취소 완료")
                    .message("정원 초과로 예약을 확정하지 못해 자동 취소되었습니다.")
                    .build();
        }

        if (order.getStatus() == OrderStatus.EXPIRED) {
            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getFinalAmount())
                    .status("EXPIRED")
                    .errorCode("ORDER_EXPIRED")
                    .errorMessage("결제 유효시간이 만료된 주문입니다.")
                    .build();
        }

        if (order.getStatus() == OrderStatus.FAILED) {
            return ConfirmResponse.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getFinalAmount())
                    .status("PAYMENT_FAILED")
                    .errorMessage(payment.getFailReason())
                    .build();
        }

        // 미확정(IN_PROGRESS / READY)인 경우 토스 단건 조회
        TossApproveResponse checkResponse = fetchTossPaymentSingle(orderNo);
        if (checkResponse != null) {
            if ("DONE".equalsIgnoreCase(checkResponse.getStatus())) {
                return completeReservationInTx(order, payment, checkResponse.getPaymentKey(), checkResponse);
            } else if ("ABORTED".equalsIgnoreCase(checkResponse.getStatus()) || "EXPIRED".equalsIgnoreCase(checkResponse.getStatus())) {
                handleOrderExpired(order, payment);
                return ConfirmResponse.builder()
                        .orderNo(order.getOrderNo())
                        .amount(order.getFinalAmount())
                        .status("EXPIRED")
                        .errorCode("ORDER_EXPIRED")
                        .build();
            }
        }

        return ConfirmResponse.builder()
                .orderNo(order.getOrderNo())
                .amount(order.getFinalAmount())
                .status("PROCESSING")
                .paymentStatus("처리 중")
                .message("결제 처리 중입니다. 잠시 후 다시 확인해 주세요.")
                .build();
    }

    // =========================================================================
    // [스케줄러 전용] IN_PROGRESS 상태인 주문을 토스 단건 조회를 통해 자동 확정
    // =========================================================================
    public void syncInProgressPayment(Payment payment) {
        Order order = payment.getOrder();
        if (order == null || payment.getStatus() != PaymentStatus.IN_PROGRESS) {
            return;
        }

        String orderNo = order.getOrderNo();
        log.info("[PaymentScheduler] IN_PROGRESS 주문 토스 단건 조회 자동 동기화 시도: orderNo={}", orderNo);

        TossApproveResponse checkResponse = fetchTossPaymentSingle(orderNo);
        if (checkResponse == null) {
            log.warn("[PaymentScheduler] 토스 단건 조회 응답 없음(처리 중 또는 네트워크 지연): orderNo={}", orderNo);
            return;
        }

        String tossStatus = checkResponse.getStatus();

        if ("DONE".equalsIgnoreCase(tossStatus)) {
            log.info("[PaymentScheduler] 토스 승인 완료 확인 -> 예약 확정 진행: orderNo={}", orderNo);
            completeReservationInTx(order, payment, checkResponse.getPaymentKey(), checkResponse);
        } else if ("ABORTED".equalsIgnoreCase(tossStatus) || "EXPIRED".equalsIgnoreCase(tossStatus)) {
            log.info("[PaymentScheduler] 토스 실패/만료 확인 -> 주문 정리: orderNo={}, tossStatus={}", orderNo, tossStatus);
            handleOrderExpired(order, payment);
        }
    }

    // =========================================================================
    // 5. [POST /payment/refund_order] 정책 기반 결제 취소/환불 API (전체 취소 & 특정 강의 선택 부분 취소 지원)
    // =========================================================================
    @Transactional
    public RefundResponse refundOrder(RefundRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderRepository.findByOrderNo(request.getOrder_no())
                .orElseThrow(() -> new ResourceNotFoundException("해당 주문을 찾을 수 없습니다: " + request.getOrder_no()));

        if (!order.getUser().getEmail().equals(email)) {
            throw new InvalidStateException("본인의 주문만 환불할 수 있습니다.");
        }

        // 1차 부분 취소(PARTIAL_REFUNDED) 후 남은 강의를 추가 취소하는 경우도 허용
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PARTIAL_REFUNDED) {
            throw new InvalidStateException("결제 완료(PAID) 또는 부분 환불(PARTIAL_REFUNDED) 상태의 주문만 환불할 수 있습니다.");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new InvalidStateException("결제 정보를 찾을 수 없습니다."));

        if (payment.getStatus() != PaymentStatus.DONE && payment.getStatus() != PaymentStatus.PARTIAL_CANCELED) {
            throw new InvalidStateException("완료된 결제만 환불할 수 있습니다.");
        }

        List<OrderItem> allOrderItems = orderItemRepository.findByOrder(order);
        if (allOrderItems.isEmpty()) {
            throw new InvalidStateException("주문 상품 내역이 존재하지 않습니다.");
        }

        // 특정 강의(order_item_id 또는 order_item_ids) 선택 여부 판별
        Set<Integer> requestedItemIds = new LinkedHashSet<>();
        if (request.getOrder_item_id() != null) {
            requestedItemIds.add(request.getOrder_item_id());
        }
        if (request.getOrder_item_ids() != null && !request.getOrder_item_ids().isEmpty()) {
            requestedItemIds.addAll(request.getOrder_item_ids());
        }

        List<OrderItem> targetItemsToRefund = new ArrayList<>();
        if (!requestedItemIds.isEmpty()) {
            // [특정 강의 선택 부분 취소] 요청한 order_item_id가 해당 주문에 속하는지 & PAID 상태인지 검증
            Map<Integer, OrderItem> itemByIdMap = new HashMap<>();
            for (OrderItem item : allOrderItems) {
                itemByIdMap.put(item.getOrderItemId(), item);
            }
            for (Integer reqItemId : requestedItemIds) {
                OrderItem matched = itemByIdMap.get(reqItemId);
                if (matched == null) {
                    throw new InvalidStateException("해당 주문에 포함되지 않은 주문 상품 ID입니다: " + reqItemId);
                }
                if (matched.getStatus() != OrderItemStatus.PAID) {
                    throw new InvalidStateException("이미 환불되었거나 취소 불가능한 강의입니다: " + matched.getClassTitle());
                }
                targetItemsToRefund.add(matched);
            }
        } else {
            // [전체 취소] 아직 결제 완료(PAID) 상태인 모든 강의 선택
            for (OrderItem item : allOrderItems) {
                if (item.getStatus() == OrderItemStatus.PAID) {
                    targetItemsToRefund.add(item);
                }
            }
            if (targetItemsToRefund.isEmpty()) {
                throw new InvalidStateException("환불 가능한 결제 완료 상태의 강의가 없습니다.");
            }
        }

        int totalRefundAmount = 0;
        int minRefundRate = 100;
        Map<OrderItem, Integer> itemRefundRateMap = new HashMap<>();
        List<Integer> refundedItemIds = new ArrayList<>();

        // 선택된 강의들에 대해 정책 기반 환불 자격 및 환불율 검증
        for (OrderItem item : targetItemsToRefund) {
            int rate = 100;
            if (item.getClazz().getClassType() == ClassType.OFFLINE) {
                ClassSchedule schedule = item.getSchedule();
                if (schedule == null) {
                    throw new InvalidStateException("오프라인 강의 일정이 존재하지 않습니다.");
                }

                LocalDate today = LocalDate.now();
                LocalDate classDate = schedule.getStartAt().toLocalDate();
                long daysBetween = ChronoUnit.DAYS.between(today, classDate);

                if (daysBetween <= 0) {
                    throw new InvalidStateException("수업 당일 또는 이미 시작된 오프라인 강의는 취소/환불이 불가합니다.");
                } else if (daysBetween == 1 || daysBetween == 2) {
                    rate = 50; // 수강일 1~2일 전 50%
                } else {
                    rate = 100; // 수강일 3일 전 100%
                }
            } else {
                // 온라인 강의
                if (payment.getApprovedAt() != null && payment.getApprovedAt().plusDays(7).isBefore(LocalDateTime.now())) {
                    throw new InvalidStateException("결제 후 7일이 경과하여 환불이 불가합니다.");
                }

                Reservation reservation = reservationRepository.findByOrderItem(item).orElse(null);
                if (reservation != null && reservation.getFirstWatchedAt() != null) {
                    throw new InvalidStateException("이미 수강을 시작한(진도율 7% 이상) 강의는 환불이 불가합니다.");
                }
                rate = 100; // 결제 후 7일 이내 & 미수강 100%
            }

            int itemRefundAmount = (int) Math.round(item.getFinalItemPrice() * (rate / 100.0));
            totalRefundAmount += itemRefundAmount;
            if (rate < minRefundRate) {
                minRefundRate = rate;
            }
            itemRefundRateMap.put(item, rate);
            refundedItemIds.add(item.getOrderItemId());
        }

        String reason = (request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : "고객 요청에 의한 결제 취소";

        // 토스페이먼츠 취소 호출 (부분 취소 또는 전액 취소, 품목 ID 기반 멱등키 적용)
        String idempotencySuffix = refundedItemIds.toString().replaceAll("[^0-9,]", "");
        cancelTossPaymentWithKey(payment.getPaymentKey(), reason, totalRefundAmount, idempotencySuffix);

        // 선택된 강의만 REFUNDED로 상태 변경 및 정원/예약 복구
        for (OrderItem item : targetItemsToRefund) {
            item.setStatus(OrderItemStatus.REFUNDED);
            orderItemRepository.save(item);

            Reservation reservation = reservationRepository.findByOrderItem(item).orElse(null);
            if (reservation != null) {
                reservation.setStatus(ReservationStatus.CANCELED);
                reservationRepository.save(reservation);
            }

            if (item.getSchedule() != null) {
                ClassSchedule schedule = classScheduleRepository.findByIdWithLock(item.getSchedule().getScheduleId()).orElse(null);
                if (schedule != null) {
                    int currentReserved = schedule.getReservedCount() != null ? schedule.getReservedCount() : 0;
                    schedule.setReservedCount(Math.max(0, currentReserved - item.getQuantity()));
                    if (schedule.getStatus() == ScheduleStatus.FULL && schedule.getReservedCount() < schedule.getCapacity()) {
                        schedule.setStatus(ScheduleStatus.OPEN);
                    }
                    classScheduleRepository.save(schedule);
                }
            }

            // Refund 엔티티 이력 생성 및 저장
            int rate = itemRefundRateMap.getOrDefault(item, 100);
            int refundAmount = (int) Math.round(item.getFinalItemPrice() * (rate / 100.0));

            Refund refund = new Refund();
            refund.setOrderItem(item);
            refund.setUser(order.getUser());
            refund.setReason(reason);
            refund.setRefundRate(rate);
            refund.setRefundAmount(refundAmount);
            refund.setStatus(RefundStatus.COMPLETED);
            refund.setProcessedAt(LocalDateTime.now());
            refundRepository.save(refund);
        }

        // 1. 주문 내 아직 결제완료(PAID)로 남아있는 강의가 있는지 확인
        boolean hasRemainingPaidItems = allOrderItems.stream()
                .anyMatch(oi -> oi.getStatus() == OrderItemStatus.PAID);

        // 2. 과거 환불 내역 합계 + 이번 환불액 합산 (DB flush 전이거나 롤백 안전성 확보)
        int previousRefundAmount = refundRepository.findByOrderItemIn(allOrderItems).stream()
                .mapToInt(Refund::getRefundAmount)
                .sum();
        int accumulatedRefundAmount = previousRefundAmount + totalRefundAmount;

        // 3. 남은 PAID 강의가 없고, 누적 환불 총액이 결제 원금과 일치(100% 전액 환불)하는 경우에 최종 REFUNDED 처리
        boolean isFullOrderRefund = !hasRemainingPaidItems && (accumulatedRefundAmount >= payment.getAmount());

        order.setStatus(isFullOrderRefund ? OrderStatus.REFUNDED : OrderStatus.PARTIAL_REFUNDED);
        payment.setStatus(isFullOrderRefund ? PaymentStatus.CANCELED : PaymentStatus.PARTIAL_CANCELED);
        orderRepository.save(order);
        paymentRepository.save(payment);

        return RefundResponse.builder()
                .orderNo(order.getOrderNo())
                .refundedOrderItemIds(refundedItemIds)
                .refundAmount(totalRefundAmount)
                .refundRate(minRefundRate)
                .status(isFullOrderRefund ? "REFUNDED" : "PARTIAL_REFUNDED")
                .message("환불이 정상적으로 완료되었습니다.")
                .build();
    }

    // 대기 중 임시 주문 취소 API (멱등성 보장)
    @Transactional
    public void cancelPendingOrder(CancelPendingRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Order> optOrder = orderRepository.findByOrderNo(request.getOrder_no());
        if (optOrder.isEmpty()) {
            return; // 이미 만료/삭제된 건은 멱등하게 200 처리
        }

        Order order = optOrder.get();
        if (!order.getUser().getEmail().equals(email)) {
            throw new InvalidStateException("본인의 주문만 취소할 수 있습니다.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            return; // 이미 결제완료/만료된 건은 무시
        }

        Payment payment = paymentRepository.findByOrder(order).orElse(null);
        if (payment != null && payment.getStatus() != PaymentStatus.READY) {
            throw new InvalidStateException("진행 중인 결제는 취소할 수 없습니다.");
        }

        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            item.setStatus(OrderItemStatus.CANCELED);
        }
        orderItemRepository.saveAll(items);

        if (payment != null) {
            payment.setStatus(PaymentStatus.ABORTED);
            payment.setFailReason("USER_CANCELED");
            paymentRepository.save(payment);
        }
    }

    // 토스 취소 API 호출 및 단건 조회
    public TossCancelResponse cancelTossPayment(String paymentKey, String cancelReason, Integer cancelAmount) {
        return cancelTossPaymentWithKey(paymentKey, cancelReason, cancelAmount, null);
    }

    public TossCancelResponse cancelTossPaymentWithKey(String paymentKey, String cancelReason, Integer cancelAmount, String itemKeySuffix) {
        String authValue = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        RestClient restClient = createTimeoutRestClient();

        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", cancelReason);
        if (cancelAmount != null) {
            body.put("cancelAmount", cancelAmount);
        }

        String idempotencyKey = "CANCEL-" + paymentKey
                + (itemKeySuffix != null && !itemKeySuffix.isEmpty() ? "-ITEMS-" + itemKeySuffix : "")
                + "-" + (cancelAmount != null ? cancelAmount : "ALL");

        try {
            return restClient.post()
                    .uri("https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + authValue)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TossCancelResponse.class);
        } catch (Exception e) {
            log.error("[TossCancel] 토스 결제 취소 API 호출 실패: paymentKey={}, error={}", paymentKey, e.getMessage());
            throw new InvalidStateException("토스 결제 취소 실패: " + e.getMessage());
        }
    }

    private TossApproveResponse fetchTossPaymentSingle(String orderNo) {
        String authValue = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        RestClient restClient = createTimeoutRestClient();

        try {
            return restClient.get()
                    .uri("https://api.tosspayments.com/v1/payments/orders/" + orderNo)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + authValue)
                    .retrieve()
                    .body(TossApproveResponse.class);
        } catch (Exception e) {
            log.warn("[TossSingleInquiry] 단건 조회 실패: orderNo={}, msg={}", orderNo, e.getMessage());
            return null;
        }
    }

    private void handleOrderExpired(Order order, Payment payment) {
        runInNewTransaction(() -> {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            List<OrderItem> items = orderItemRepository.findByOrder(order);
            for (OrderItem item : items) {
                item.setStatus(OrderItemStatus.EXPIRED);
            }
            orderItemRepository.saveAll(items);

            payment.setStatus(PaymentStatus.ABORTED);
            payment.setFailReason("ORDER_EXPIRED");
            paymentRepository.save(payment);
        });
    }

    private void handlePaymentFailed(Order order, Payment payment, String paymentKey, String message) {
        runInNewTransaction(() -> {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            List<OrderItem> items = orderItemRepository.findByOrder(order);
            for (OrderItem item : items) {
                item.setStatus(OrderItemStatus.FAILED);
            }
            orderItemRepository.saveAll(items);

            payment.setPaymentKey(paymentKey);
            payment.setStatus(PaymentStatus.ABORTED);
            payment.setFailReason(message != null && message.length() > 250 ? message.substring(0, 250) : message);
            paymentRepository.save(payment);
        });
    }

    private void handlePaymentInProgress(Payment payment, String paymentKey) {
        runInNewTransaction(() -> {
            payment.setPaymentKey(paymentKey);
            payment.setStatus(PaymentStatus.IN_PROGRESS);
            paymentRepository.save(payment);
        });
    }

    private String buildOrderName(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return "클래스 결제";
        }
        return (orderItems.size() > 1)
                ? orderItems.get(0).getClassTitle() + " 외 " + (orderItems.size() - 1) + "건"
                : orderItems.get(0).getClassTitle();
    }

    private ConfirmResponse buildCompletedResponseFromDb(Order order, Payment payment, List<OrderItem> orderItems) {
        return ConfirmResponse.builder()
                .orderNo(order.getOrderNo())
                .orderName(buildOrderName(orderItems))
                .amount(order.getFinalAmount())
                .status("COMPLETED")
                .paymentMethod(payment.getMethod() != null ? payment.getMethod() : "신용카드")
                .reservationStatus("예약 확정")
                .receiptUrl(payment.getReceiptUrl())
                .approvedAt(payment.getApprovedAt() != null ? payment.getApprovedAt().toString() : null)
                .build();
    }

    private String formatPaymentMethod(TossApproveResponse toss) {
        if (toss == null) return "결제 완료";
        if (toss.getCard() != null) {
            String issuer = mapCardIssuerName(toss.getCard().getIssuerCode());
            String number = toss.getCard().getNumber() != null ? toss.getCard().getNumber() : "";
            return "신용카드 (" + issuer + (number.isEmpty() ? "" : " " + number) + ")";
        } else if (toss.getEasyPay() != null) {
            return "간편결제 (" + toss.getEasyPay().getProvider() + ")";
        } else if (toss.getMethod() != null) {
            return toss.getMethod();
        }
        return "신용카드";
    }

    private String mapCardIssuerName(String issuerCode) {
        if (issuerCode == null || issuerCode.isBlank()) {
            return "카드";
        }
        return switch (issuerCode.trim()) {
            case "11", "06", "CN" -> "국민";
            case "41", "SH" -> "신한";
            case "61", "HD" -> "현대";
            case "51", "SS" -> "삼성";
            case "71", "LT" -> "롯데";
            case "31", "BC" -> "비씨";
            case "21", "HN" -> "하나";
            case "91", "NH" -> "농협";
            case "33", "WR" -> "우리";
            case "15", "KK" -> "카카오뱅크";
            case "24", "TB" -> "토스뱅크";
            default -> issuerCode;
        };
    }
}
