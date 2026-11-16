package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Integer> {
    // 오프라인 최대 정원 = 정원이 가장 큰 일정 1개
    Optional<ClassSchedule> findTopByClazz_ClassIdOrderByCapacityDesc(Integer classId);
}