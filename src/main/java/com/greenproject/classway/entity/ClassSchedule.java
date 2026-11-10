package com.greenproject.classway.entity;

import com.greenproject.classway.global.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_schedules")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClassSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity clazz;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "running_time")
    private LocalTime runningTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "reserved_count", nullable = false)
    private Integer reservedCount = 0;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ScheduleStatus status = ScheduleStatus.OPEN;
}
