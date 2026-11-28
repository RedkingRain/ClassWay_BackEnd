package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Order;
import com.greenproject.classway.entity.Payment;
import com.greenproject.classway.global.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    void deleteByOrderInAndStatus(List<Order> orders, PaymentStatus status);
    Optional<Payment> findByOrder(Order order);
    List<Payment> findByStatus(PaymentStatus status);
}
