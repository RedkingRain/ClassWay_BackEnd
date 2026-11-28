package com.greenproject.classway.entity;

import com.greenproject.classway.global.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "coupon_discount", nullable = false)
    private Integer couponDiscount = 0;

    @Column(name = "point_used", nullable = false)
    private Integer pointUsed = 0;

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "ordered_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime orderedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
