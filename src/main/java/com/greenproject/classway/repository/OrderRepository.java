package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Order;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.global.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUserAndStatus(User user, OrderStatus status);
    Optional<Order> findByOrderNo(String orderNo);
    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime now);
}
