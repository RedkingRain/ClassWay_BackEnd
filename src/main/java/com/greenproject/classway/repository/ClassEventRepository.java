package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClassEventRepository extends JpaRepository<ClassEvent, Integer> {
    // 여러 클래스의 [지금 노출 중인] 이벤트 배지를 한 번에
    @Query("""
            SELECT e FROM ClassEvent e
            WHERE e.clazz.classId IN :classIds
              AND e.isActive = true
              AND e.startAt <= :now
              AND e.endAt >= :now
            """)
    List<ClassEvent> findActiveEvents(@Param("classIds") List<Integer> classIds,
                                      @Param("now") LocalDateTime now);
}
