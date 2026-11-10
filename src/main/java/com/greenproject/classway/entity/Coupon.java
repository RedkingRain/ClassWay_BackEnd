package com.greenproject.classway.entity;

import com.greenproject.classway.global.CouponDiscountType;
import com.greenproject.classway.global.CouponIssueType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Integer couponId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "issue_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CouponIssueType issueType = CouponIssueType.DOWNLOAD;

    @Column(length = 30, unique = true)
    private String code;

    @Column(name = "discount_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CouponDiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    @Column(name = "min_order_amount", nullable = false)
    private Integer minOrderAmount = 0;

    @Column(name = "max_discount")
    private Integer maxDiscount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
