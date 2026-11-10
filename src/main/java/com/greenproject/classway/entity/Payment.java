package com.greenproject.classway.entity;

import com.greenproject.classway.global.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(length = 20)
    private String method;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.READY;

    @Column(name = "fail_reason", length = 255)
    private String failReason;

    @Column(name = "receipt_url", length = 255)
    private String receiptUrl;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
