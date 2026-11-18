package com.greenproject.classway.service;

import com.greenproject.classway.dto.AdminTutorListResponse;
import com.greenproject.classway.entity.Tutor;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.AuthorizationFailureException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.TutorApprovalStatus;
import com.greenproject.classway.global.UserRole;
import com.greenproject.classway.repository.TutorRepository;
import com.greenproject.classway.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    tutor.getApprovalStatus().name()    // 상태 (예: "PENDING)

            ));
        }
        // 4. 완성된 목록 돌려주기 (하나도 없으면 빈 목록 [] -> 오류 아님)
        return result;
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
}
