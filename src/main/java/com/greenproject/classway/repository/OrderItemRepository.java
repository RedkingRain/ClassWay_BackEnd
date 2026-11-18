package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Order;
import com.greenproject.classway.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    void deleteByOrderIn(List<Order> orders);
    List<OrderItem> findByOrder(Order order);

    @Query(value = """
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM order_items oi
        WHERE oi.schedule_id = :scheduleId
          AND oi.status IN ('PENDING', 'PAID')
    """, nativeQuery = true)
    int countReservedCapacity(@Param("scheduleId") Integer scheduleId);
}
