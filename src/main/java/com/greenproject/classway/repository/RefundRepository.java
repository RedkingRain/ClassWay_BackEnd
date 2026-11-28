package com.greenproject.classway.repository;

import com.greenproject.classway.entity.OrderItem;
import com.greenproject.classway.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Integer> {
    List<Refund> findByOrderItemIn(Collection<OrderItem> orderItems);
}
