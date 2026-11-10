package com.greenproject.classway.entity;

import com.greenproject.classway.global.UserCouponStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserCoupon {
    @EmbeddedId
    private UserCouponId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("couponId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserCouponStatus status = UserCouponStatus.AVAILABLE;

    @Column(name = "issued_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
