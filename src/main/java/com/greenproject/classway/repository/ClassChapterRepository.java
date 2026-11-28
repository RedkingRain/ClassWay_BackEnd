package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassChapter;
import com.greenproject.classway.entity.ClassChapterId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassChapterRepository extends JpaRepository<ClassChapter, ClassChapterId> {
    // 클래스의 챕터 목록 (정해둔 순서대로)
    List<ClassChapter> findByClazz_ClassIdOrderBySortOrderAsc(Integer classId);

    // 클래스 내 현재 가장 큰 챕터 번호 조회 (없으면 0)
    @Query("SELECT COALESCE(MAX(c.chapterId), 0) FROM ClassChapter c WHERE c.clazz.classId = :classId")
    Integer findMaxChapterIdByClassId(@Param("classId") Integer classId);

    // 클래스 번호와 삭제할 챕터 번호 목록으로 챕터 삭제
    @Modifying
    @Query("DELETE FROM ClassChapter c WHERE c.clazz.classId = :classId AND c.chapterId IN :chapterIds")
    void deleteByClassIdAndChapterIdIn(@Param("classId") Integer classId, @Param("chapterIds") List<Integer> chapterIds);
}
