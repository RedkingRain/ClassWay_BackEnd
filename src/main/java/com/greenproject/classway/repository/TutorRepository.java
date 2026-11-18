package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Tutor;
import com.greenproject.classway.global.TutorApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Integer> {
    Optional<Tutor> findByUserId(Integer userId);

    // 승인 상태별 강사 신청 목록 (신청일 오래된 순)- 관리자 페이지의 강사 승인 화면용
    List<Tutor>
    findByApprovalStatusOrderByAppliedAtAsc(TutorApprovalStatus approvalStatus);
}
