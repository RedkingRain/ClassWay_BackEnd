package com.greenproject.classway.entity;

import com.greenproject.classway.global.OrderItemStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Integer orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ClassSchedule schedule;

    @Column(name = "class_title", nullable = false, length = 100)
    private String classTitle;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderItemStatus status = OrderItemStatus.PAID;
}
