package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Tutor;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.global.TutorApprovalStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Integer> {
    Optional<Tutor> findByUserId(Integer userId);

    // 로그인 이메일로 강사 정보 조회
    Optional<Tutor> findByUser_Email(String email);

    // 승인 상태별 강사 신청 목록 (신청일 오래된 순)- 관리자 페이지의 강사 승인 화면용
    // @EntityGraph: 신청자(users)를 JOIN으로 한 번에 같이 가져옴 (신청서마다 users를 따로 조회하는 N+1 방지)
    @EntityGraph(attributePaths = "user")
    List<Tutor>
    findByApprovalStatusOrderByAppliedAtAsc(TutorApprovalStatus approvalStatus);

    // 승인 상태별 신청 건수 - 관리자 강사 승인 화면의 탭 숫자용
    // 결과 한 줄 = [상태(TutorApprovalStatus), 건수(Long)], 0건인 상태는 결과에 없음
    @Query("SELECT t.approvalStatus, COUNT(t) FROM Tutor t GROUP BY t.approvalStatus")
    List<Object[]> countGroupByApprovalStatus();

    // 승인 대기(PENDING)인 신청만 처리 결과로 바꿈 (승인·반려 공통)
    // WHERE에 PENDING 조건을 걸어서, 관리자 두 명이 동시에 눌러도 한 명만 성공함
    // 반환값: 바뀐 행 수 (1 = 성공, 0 = 그 사이 다른 관리자가 먼저 처리함)
    @Modifying
    @Query("UPDATE Tutor t " +
            "SET t.approvalStatus = :result, t.rejectReason = :rejectReason, " +
            "    t.approvedBy = :admin, t.approvedAt = :processedAt " +
            "WHERE t.tutorId = :tutorId " +
            "AND t.approvalStatus = com.greenproject.classway.global.TutorApprovalStatus.PENDING")
    int processPendingApplication(@Param("tutorId") Integer tutorId,
                                  @Param("result") TutorApprovalStatus result,
                                  @Param("rejectReason") String rejectReason,
                                  @Param("admin") User admin,
                                  @Param("processedAt") LocalDateTime processedAt);
}
