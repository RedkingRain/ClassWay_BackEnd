package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassSchedule;
import com.greenproject.classway.global.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Integer> {
    // 오프라인 최대 정원 = 정원이 가장 큰 일정 1개
    Optional<ClassSchedule> findTopByClazz_ClassIdOrderByCapacityDesc(Integer classId);

    // 예약 화면용 일정: 이 클래스의, 지금 이후 시작하는, 정해둔 상태(OPEN · FULL)만, 시작 빠른 순
    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.clazz.classId = :classId
              AND s.startAt > :now
              AND s.status IN :statuses
            ORDER BY s.startAt ASC
            """)
    List<ClassSchedule> findUpcomingSchedules(
            @Param("classId") Integer classId,
            @Param("now") LocalDateTime now,
            @Param("statuses") List<ScheduleStatus> statuses);

    // 오프라인 회차 집계용 프로젝션 인터페이스
    interface OfflineSummaryProjection {
        Integer getClassId();
        Long getScheduleCount();
        Long getTotalReserved();
        Long getTotalCapacity();
    }

    // 여러 클래스의 회차 수, 예약수 합, 정원 합을 1번에 조회
    @Query("""
            SELECT s.clazz.classId AS classId,
                   COUNT(s) AS scheduleCount,
                   COALESCE(SUM(s.reservedCount), 0) AS totalReserved,
                   COALESCE(SUM(s.capacity), 0) AS totalCapacity
            FROM ClassSchedule s
            WHERE s.clazz.classId IN :classIds
            GROUP BY s.clazz.classId
            """)
    List<OfflineSummaryProjection> summarizeSchedulesByClassIds(@Param("classIds") List<Integer> classIds);
}