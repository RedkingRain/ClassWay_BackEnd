package com.greenproject.classway.repository;

import com.greenproject.classway.entity.StudyPost;
import com.greenproject.classway.global.StudyPostType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyPostRepository extends JpaRepository<StudyPost, Integer> {
    // 학습 게시판 목록: 공지를 맨 위에, 같은 구분 안에서는 최신순
    @Query("""
            SELECT p
            FROM StudyPost p
            WHERE p.clazz.classId = :classId
            ORDER BY CASE WHEN p.type = :notice THEN 0 ELSE 1 END, p.createdAt DESC, p.postId DESC
            """)
    List<StudyPost> findBoardList(@Param("classId") Integer classId,
                                  @Param("notice") StudyPostType notice);
}