package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Integer> {
    // 클래스의 강의 전체 (챕터 번호는 클래스마다 겹쳐서, 강의에 적힌 클래스 번호로 찾음)
    List<Lecture> findByClassIdOrderBySortOrderAsc(Integer classId);

    // 특정 클래스에서 특정 챕터 번호들에 속한 강의 전체 삭제
    @Modifying
    @Query("DELETE FROM Lecture l WHERE l.classId = :classId AND l.chapterId IN :chapterIds")
    void deleteByClassIdAndChapterIdIn(@Param("classId") Integer classId, @Param("chapterIds") List<Integer> chapterIds);

    // 특정 클래스에서 특정 강의 번호들에 해당하는 강의 삭제
    @Modifying
    @Query("DELETE FROM Lecture l WHERE l.classId = :classId AND l.lectureId IN :lectureIds")
    void deleteByClassIdAndLectureIdIn(@Param("classId") Integer classId, @Param("lectureIds") List<Integer> lectureIds);

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

    // 온라인 VOD 강의 수 집계용 프로젝션 인터페이스
    interface LectureCountProjection {
        Integer getClassId();
        Long getLectureCount();
    }

    // 여러 클래스의 VOD 강의 수를 한 번에 조회
    @Query("""
            SELECT l.chapter.clazz.classId AS classId, COUNT(l) AS lectureCount
            FROM Lecture l
            WHERE l.chapter.clazz.classId IN :classIds
            GROUP BY l.chapter.clazz.classId
            """)
    List<LectureCountProjection> countLecturesByClassIds(@Param("classIds") List<Integer> classIds);
}