package com.greenproject.classway.repository;

import com.greenproject.classway.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import com.greenproject.classway.entity.UserCouponId;

public interface UserCouponRepository extends JpaRepository<UserCoupon, UserCouponId> {
}
