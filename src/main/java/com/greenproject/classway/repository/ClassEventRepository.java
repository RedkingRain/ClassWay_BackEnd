package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.greenproject.classway.global.ClassStatus;
import org.springframework.data.domain.Pageable;

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

    // 지금 노출 중인 이벤트의 클래스 번호 (공개 클래스만, 마감 임박순 → 같으면 최신 등록순)
    // 몇 개까지 꺼낼지는 pageable로 정함 (8개 → PageRequest.of(0, 8))
    @Query("""
            SELECT e.clazz.classId
            FROM ClassEvent e
            WHERE e.isActive = true
              AND e.startAt <= :now
              AND e.endAt >= :now
              AND e.clazz.status = :published
            ORDER BY e.endAt ASC, e.eventId DESC
            """)
    List<Integer> findEventClassIds(@Param("now") LocalDateTime now,
                                    @Param("published") ClassStatus published,
                                    Pageable pageable);
}
