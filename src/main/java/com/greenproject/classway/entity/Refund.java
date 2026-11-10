package com.greenproject.classway.entity;

import com.greenproject.classway.global.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Integer refundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "refund_rate", nullable = false)
    private Integer refundRate;

    @Column(name = "refund_amount", nullable = false)
    private Integer refundAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "requested_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
