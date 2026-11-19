package com.greenproject.classway.service;

import com.greenproject.classway.dto.AdminTutorListResponse;
import com.greenproject.classway.dto.TutorRejectRequest;
import com.greenproject.classway.entity.Tutor;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.AuthorizationFailureException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.exceptions.InvalidStateException;
import com.greenproject.classway.global.TutorApprovalStatus;
import com.greenproject.classway.global.UserRole;
import com.greenproject.classway.repository.TutorRepository;
import com.greenproject.classway.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 페이지 기능의 실제 처리(비즈니스 로직)를 담당하는 클래스
 * - 강사 승인·반려 (ADMIN-002)
 * - 모든 메서드는 첫 줄에서 findLoginAdmin()으로 관리자인지 검사 (아니면 403)
 */
@Service
@AllArgsConstructor
public class AdminService {
    private final UserRepository userRepository;    // 로그인한 사용자가 관리자인지 확인용
    private final TutorRepository tutorRepository;  // 강사 신청서 조회·수정용

    //====================================
    // 강사 신청 목록 조회 (ADMIN-002)
    //====================================
    /**
     * 승인 상태별 강사 신청 목록 조회. (승인 대기/ 승인/ 반려)
     * @param status 조회할 상태 (PENDING/ APPROVED/ REJECTED)
     * @return 신청자 목록 (없으면 빈 목록)
     */
    @Transactional(readOnly = true)
    public List<AdminTutorListResponse>
    getTutorApplications(TutorApprovalStatus status) {

        // 1. 관리자인지 확인 (아니면 403, 여기서 멈춤)
        findLoginAdmin();

        // 2. 해당 상태의 신청서들을 DB에서 꺼냄. (신청일 오래된 순으로)
        List<Tutor> tutors =
                tutorRepository.findByApprovalStatusOrderByAppliedAtAsc(status);

        // 3. 엔티티(Tutor) -> 필요한 정보만 응답 DTO에 하나씩 옮겨 담기

        List<AdminTutorListResponse> result = new ArrayList<>(); // 빈 목록 상자 준비
        for (Tutor tutor : tutors) {
            User applicant = tutor.getUser();
            result.add(new AdminTutorListResponse(
                    tutor.getTutorId(),                 // 신청 번호
                    applicant.getName(),                // 이름 (users)
                    applicant.getEmail(),               // 이메일 (users)
                    tutor.getAppliedAt(),               // 신청일 (tutors)
                    tutor.getApprovalStatus().name()    // 상태 (예: "PENDING")

            ));
        }
        // 4. 완성된 목록 돌려주기 (하나도 없으면 빈 목록 [] -> 오류 아님)
        return result;
    }
    //====================================
    // 강사 승인 (ADMIN-002)
    //====================================
    /**
     * 승인 대기 중인 강사 신청을 승인한다.
     * - role은 가입 때 이미 TUTOR라서 바꾸지 않음
     * @param tutorId 승인할 신청 번호
     */
    @Transactional
    public void approveTutor(Integer tutorId) {
        // 1. 관리자인지 확인 (아니면 403) -> tutors 테이블에 처리한 관리자 기록용
        User admin = findLoginAdmin();

        // 2. 신청서가 있는지 (404), 아직 승인 대기인지(400) 확인
        Tutor tutor = findPendingTutor(tutorId);

        // 3. 승인 처리 (tutors 테이블의 칸 3개 변경)
        tutor.setApprovalStatus(TutorApprovalStatus.APPROVED); // 상태: 승인 대기 -> 승인
        tutor.setApprovedBy(admin);                            // 처리한 관리자
        tutor.setApprovedAt(LocalDateTime.now());               // 처리 시간

        // 4. tutors 테이블에 저장
        tutorRepository.save(tutor);

        // TODO: AlarmService 만든 뒤 신청자에게 승인 알림 보내기
    }
    //====================================
    // 강사 반려 (ADMIN-002)
    //====================================
    /**
     * 승인 대기 중인 강사 신청을 반려한다
     * - 반려 사유가 비었는지는 Controller의 @Valid가 먼저 검사함(400)
     * - approved_by, approved_at의 이름은 "승인"이지만 반려한 관리자·시간도 함께 기록하는 칸임
     * @param tutorId 반려할 신청 번호
     * @param request 반려 사유
     */
    @Transactional
    public void rejectTutor(Integer tutorId, TutorRejectRequest request) {
        // 1. 관리자인지 확인 (아니면 403) -> 해당 작업 처리한 관리자 기록용
        User admin = findLoginAdmin();

        // 2. 신청서가 있는지(404), 아직 승인 대기인지(400) 확인 -> 승인과 같은 공통 메서드 재사용
        Tutor tutor = findPendingTutor(tutorId);

        // 3. 반려 처리 (tutors 테이블의 칸 4개 변경)
        tutor.setApprovalStatus(TutorApprovalStatus.REJECTED); // 상태: 승인 대기 -> 반려
        tutor.setRejectReason(request.getRejectReason());      // 반려 사유
        tutor.setApprovedBy(admin);                            // 처리한 관리자
        tutor.setApprovedAt(LocalDateTime.now());              // 처리 시간

        // 4. tutors 테이블에 저장
        tutorRepository.save(tutor);

        // TODO: AlarmService 만든 뒤 신청자에게 반려 알림 보내기
    }

    //====================================
    // 공통 메서드 (관리자 기능에서 함께 사용)
    //====================================
    /**
     * 현재 로그인한 사용자가 관리자인지 확인하고 값을 돌려준다
     * @throws ResourceNotFoundException DB에 해당 사용자가 없을 때 (404)
     * @throws AuthorizationFailureException 관리자가 아닐 때 (403)
     */
    private User findLoginAdmin() {
        // 1. JWT 필터가 SecurityContext에 저장해 둔 로그인 사용자 이메일 꺼내기
        String email =
                SecurityContextHolder.getContext().getAuthentication().getName();
        // 2. 이메일로 DB 조회 -> 있으면 꺼내고, 없으면 404
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 계정입니다."));

        // 3. 역할이 ADMIN이 아니면 403
        if (user.getRole() != UserRole.ADMIN) {
            throw new AuthorizationFailureException("관리자만 이용할 수 있습니다.");
        }
        return user;
    }
    /**
     * 신청 번호로 강사 신청서를 찾는다.
     * @param tutorId 신청 번호 (tutors.tutor_id)
     * @throws ResourceNotFoundException 해당 번호의 신청서가 없을 때 (404)
     */
    private Tutor findTutor(Integer tutorId) {
        // 신청 번호로 DB 조회 -> 있으면 꺼내고, 없으면 404
        return tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 강사 신청입니다."));
    }
    /**
     * 강사 신청서를 찾고, 아직 처리 전(승인 대기) 인지 확인한다. (승인·반려 공통)
     * @param tutorId 신청 번호
     * @throws ResourceNotFoundException 신청서가 없을 때 (404)
     * @throws InvalidStateException      이미 승인·반려된 신청일 때 (400)
     */
    private Tutor findPendingTutor(Integer tutorId) {
        // 1. 신청서 찾기 (없으면 404)
        Tutor tutor = findTutor(tutorId);

        // 2. 승인 대기(PENDING)가 아니면 = 이미 승인 또는 반려됨 -> 400
        if (tutor.getApprovalStatus() != TutorApprovalStatus.PENDING) {
            throw new InvalidStateException("이미 처리된 강사 신청입니다.");
        }
        // 3. 처리 가능한 신청서 돌려주기
        return tutor;
    }
}
