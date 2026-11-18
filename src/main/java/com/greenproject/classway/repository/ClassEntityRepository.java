package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassEntityRepository extends JpaRepository<ClassEntity, Integer> {
    // 번호 + 상태로 클래스 찾기 → 상세는 PUBLISHED(공개)만 보여줌
    Optional<ClassEntity> findByClassIdAndStatus(Integer classId, ClassStatus status);

    // 강사의 공개 클래스 수 (강사 카드 [클래스 N개 운영])
    long countByTutor_TutorIdAndStatus(Integer tutorId, ClassStatus status);

    // 공개 클래스 목록 (전체 탭) - 카테고리,강사,강사회원 정보를 같이 꺼내옴
    @EntityGraph(attributePaths = {"category", "tutor", "tutor.user"})
    Page<ClassEntity> findByStatus(ClassStatus classStatus, Pageable pageable);

    // 공개 클래스 목록 ( 온라인/오프라인 탭 )
    @EntityGraph(attributePaths = {"category", "tutor", "tutor.user"})
    Page<ClassEntity> findByStatusAndClassType(ClassStatus classStatus, ClassType classType, Pageable pageable);
}
