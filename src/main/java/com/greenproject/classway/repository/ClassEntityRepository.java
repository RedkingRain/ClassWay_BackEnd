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
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface ClassEntityRepository extends JpaRepository<ClassEntity, Integer> {
    // 번호 + 상태로 클래스 찾기 → 상세는 PUBLISHED(공개)만 보여줌
    Optional<ClassEntity> findByClassIdAndStatus(Integer classId, ClassStatus status);

    // 강사의 공개 클래스 수 (강사 카드 [클래스 N개 운영])
    long countByTutor_TutorIdAndStatus(Integer tutorId, ClassStatus status);

    // 강사 ID로 등록된 클래스 페이징 조회 (최신 등록순)
    Page<ClassEntity> findByTutor_TutorId(Integer tutorId, Pageable pageable);

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

    // 찜 수 +1: DB가 직접 더해서 동시에 눌러도 숫자가 틀어지지 않음
    @Modifying
    @Query("UPDATE ClassEntity c SET c.wishCount = c.wishCount + 1 WHERE c.classId = :classId")
    int increaseWishCount(@Param("classId") Integer classId);

    // 찜 수 -1: 0보다 클 때만 (음수 방지)
    @Modifying
    @Query("UPDATE ClassEntity c SET c.wishCount = c.wishCount - 1 WHERE c.classId = :classId AND c.wishCount > 0")
    int decreaseWishCount(@Param("classId") Integer classId);

    // 찜 수 칸 값: 목록, 상세와 같은 숫자를 돌려주려고 DB에서 새로 읽음
    @Query("SELECT c.wishCount FROM ClassEntity c WHERE c.classId = :classId")
    Integer findWishCount(@Param("classId") Integer classId);
}
