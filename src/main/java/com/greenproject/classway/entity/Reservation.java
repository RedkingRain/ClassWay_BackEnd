package com.greenproject.classway.entity;

import com.greenproject.classway.global.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Integer reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ClassSchedule schedule;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Column(name = "reserved_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime reservedAt;

    @Column(name = "first_watched_at")
    private LocalDateTime firstWatchedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
