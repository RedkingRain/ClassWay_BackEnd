package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Integer> {
    // 내 알림 최신 20개 (알림 번호가 클수록 최근)
    List<Alarm> findTop20ByUser_IdOrderByAlarmIdDesc(Integer userId);

    // 내 안 읽은 알림 개수
    long countByUser_IdAndIsReadFalse(Integer userId);

    // 알림 1개 — 번호와 주인이 둘 다 맞아야 찾음 (남의 알림은 「없음」)
    Optional<Alarm> findByAlarmIdAndUser_Id(Integer alarmId, Integer userId);

    // 내 안 읽은 알림 한 번에 읽음 처리
    @Modifying
    @Query("UPDATE Alarm a SET a.isRead = true WHERE a.user.id = :userId AND a.isRead = false")
    int markAllAsRead(@Param("userId") Integer userId);
}