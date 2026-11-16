package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassChapterRepository extends JpaRepository<ClassChapter, Integer> {
    // 클래스의 챕터 목록 (정해둔 순서대로)
    List<ClassChapter> findByClazz_ClassIdOrderBySortOrderAsc(Integer classId);
}
