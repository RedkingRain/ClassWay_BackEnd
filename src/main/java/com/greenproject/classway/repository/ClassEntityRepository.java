package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.global.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassEntityRepository extends JpaRepository<ClassEntity, Integer> {
    // 번호 + 상태로 클래스 찾기 → 상세는 PUBLISHED(공개)만 보여줌
    Optional<ClassEntity> findByClassIdAndStatus(Integer classId, ClassStatus status);

    // 강사의 공개 클래스 수 (강사 카드 [클래스 N개 운영])
    long countByTutor_TutorIdAndStatus(Integer tutorId, ClassStatus status);
}
