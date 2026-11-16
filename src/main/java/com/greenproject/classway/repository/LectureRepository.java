package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Integer> {
    // 클래스의 강의 전체를 한 번에 (강의 -> 챕터 -> 클래스 번호로 찾기)
    List<Lecture> findByChapter_Clazz_ClassIdOrderBySortOrderAsc(Integer classId);
}
