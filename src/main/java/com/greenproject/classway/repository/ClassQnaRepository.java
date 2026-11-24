package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassQna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassQnaRepository extends JpaRepository<ClassQna, Integer> {
    // 클래스 상세 Q&A 목록 (CLASS-003): 삭제 안 된 문의만 최신순
    // @EntityGraph: 작성자(users)를 같이 꺼내서 문의마다 작성자를 따로 조회하는 N+1 방지
    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT q FROM ClassQna q
            WHERE q.clazz.classId = :classId
              AND q.isDeleted = false
            ORDER BY q.qnaId DESC
            """)
    Page<ClassQna> findQnaList(@Param("classId") Integer classId, Pageable pageable);
}