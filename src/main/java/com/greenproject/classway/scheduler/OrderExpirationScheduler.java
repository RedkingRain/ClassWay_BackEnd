package com.greenproject.classway.scheduler;

import com.greenproject.classway.entity.Order;
import com.greenproject.classway.entity.OrderItem;
import com.greenproject.classway.entity.Payment;
import com.greenproject.classway.global.OrderItemStatus;
import com.greenproject.classway.global.OrderStatus;
import com.greenproject.classway.global.PaymentStatus;
import com.greenproject.classway.repository.OrderItemRepository;
import com.greenproject.classway.repository.OrderRepository;
import com.greenproject.classway.repository.PaymentRepository;
import com.greenproject.classway.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void scheduleTasks() {
        // 1. 10분 유효시간 만료된 PENDING 대기 주문 정리
        cleanupExpiredOrders();

        // 2. 사용자가 창을 닫아버린 IN_PROGRESS(처리 중 지연) 주문 토스 자동 동기화
        syncPendingInProgressOrders();
    }

    @Transactional
    public void cleanupExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findByStatusAndExpiresAtBefore(OrderStatus.PENDING, now);

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (Order order : expiredOrders) {
            Payment payment = paymentRepository.findByOrder(order).orElse(null);
            // 승인 확인 중(IN_PROGRESS)이거나 완료(DONE)된 주문은 만료 스킵 (syncPendingInProgressOrders에서 처리)
            if (payment != null && (payment.getStatus() == PaymentStatus.IN_PROGRESS || payment.getStatus() == PaymentStatus.DONE)) {
                continue;
            }

            // 1. 주문 상태 EXPIRED
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            // 2. 주문 상품 상태 EXPIRED (오프라인 정원 자동 반환)
            List<OrderItem> items = orderItemRepository.findByOrder(order);
            for (OrderItem item : items) {
                item.setStatus(OrderItemStatus.EXPIRED);
            }
            orderItemRepository.saveAll(items);

            // 3. 결제 상태 ABORTED
            if (payment != null) {
                payment.setStatus(PaymentStatus.ABORTED);
                payment.setFailReason("ORDER_EXPIRED");
                paymentRepository.save(payment);
            }

            log.info("[OrderExpirationScheduler] 10분 유효시간 만료 주문 처리 완료: orderNo={}", order.getOrderNo());
        }
    }

    public void syncPendingInProgressOrders() {
        List<Payment> inProgressPayments = paymentRepository.findByStatus(PaymentStatus.IN_PROGRESS);
        if (inProgressPayments.isEmpty()) {
            return;
        }

        log.info("[OrderExpirationScheduler] 처리 중(IN_PROGRESS) 주문 {}건 발견 -> 토스 자동 동기화 시작", inProgressPayments.size());
        for (Payment payment : inProgressPayments) {
            try {
                paymentService.syncInProgressPayment(payment);
            } catch (Exception e) {
                log.error("[OrderExpirationScheduler] IN_PROGRESS 주문 동기화 중 오류 발생: orderNo={}, error={}",
                        payment.getOrder() != null ? payment.getOrder().getOrderNo() : "UNKNOWN", e.getMessage());
            }
        }
    }
}
