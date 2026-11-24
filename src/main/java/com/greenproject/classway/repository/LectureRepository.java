package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Integer> {
    // 클래스의 강의 전체 (챕터 번호는 클래스마다 겹쳐서, 강의에 적힌 클래스 번호로 찾음)
    List<Lecture> findByClassIdOrderBySortOrderAsc(Integer classId);

    // 결과 한 줄: 클래스 번호 + 강의 길이 합계(초)
    interface ClassDurationRow {
        Integer getClassId();

        Long getTotalSeconds();
    }

    // 여러 클래스의 강의 길이 합계를 한 번에 (클래스 번호별로 묶어서)
    @Query("""
            SELECT l.classId AS classId, SUM(l.durationSec) AS totalSeconds
            FROM Lecture l
            WHERE l.classId IN :classIds
            GROUP BY l.classId
            """)
    List<ClassDurationRow> sumDurationByClassIds(@Param("classIds") List<Integer> classIds);
}