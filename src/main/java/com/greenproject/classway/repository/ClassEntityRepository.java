package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassEntityRepository extends JpaRepository<ClassEntity, Integer> {
    // 번호 + 상태로 클래스 찾기 → 상세는 PUBLISHED(공개)만 보여줌
    Optional<ClassEntity> findByClassIdAndStatus(Integer classId, ClassStatus status);

    // 강사의 공개 클래스 수 (강사 카드 [클래스 N개 운영])
    long countByTutor_TutorIdAndStatus(Integer tutorId, ClassStatus status);

    // 공개 클래스 검색 (MAIN-002): 조건 값이 null이면 그 조건은 통과 (조건 안 씀)
    // keyword: 제목, 한 줄 소개, 강사 닉네임 포함 / region: 앞부분 일치 (예: '서울' → '서울 마포구')
    @EntityGraph(attributePaths = {"category", "tutor", "tutor.user"})
    @Query("""
            SELECT c FROM ClassEntity c
            WHERE c.status = :status
              AND (:keyword IS NULL
                   OR c.title LIKE CONCAT('%', :keyword, '%')
                   OR c.summary LIKE CONCAT('%', :keyword, '%')
                   OR c.tutor.user.nickname LIKE CONCAT('%', :keyword, '%'))
              AND (:categoryId IS NULL OR c.category.categoryId = :categoryId)
              AND (:classType IS NULL OR c.classType = :classType)
              AND (:region IS NULL OR c.region LIKE CONCAT(:region, '%'))
            """)
    Page<ClassEntity> searchClasses(@Param("status") ClassStatus status,
                                    @Param("keyword") String keyword,
                                    @Param("categoryId") Integer categoryId,
                                    @Param("classType") ClassType classType,
                                    @Param("region") String region,
                                    Pageable pageable);

    // 번호 목록에 있는 클래스 한 번에 (카테고리,강사 같이) — 순서는 보장 안 됨
    @EntityGraph(attributePaths = {"category", "tutor", "tutor.user"})
    List<ClassEntity> findByClassIdIn(List<Integer> classIds);

    // 공개 클래스 번호 전부 (전체 탭 랜덤용) — 번호만 꺼내서 가벼움
    @Query("SELECT c.classId FROM ClassEntity c WHERE c.status = :status")
    List<Integer> findIdsByStatus(@Param("status") ClassStatus status);

    // 공개 클래스 번호 전부 (온라인 / 오프라인 탭 랜덤용)
    @Query("""
            SELECT c.classId FROM ClassEntity c
            WHERE c.status = :status
              AND c.classType = :classType
            """)
    List<Integer> findIdsByStatusAndClassType(@Param("status") ClassStatus status,
                                              @Param("classType") ClassType classType);
}
