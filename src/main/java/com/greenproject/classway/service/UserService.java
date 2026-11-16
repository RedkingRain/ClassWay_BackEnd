package com.greenproject.classway.service;

import com.greenproject.classway.dto.MyProfileResponse;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.repository.UserRepository;
import com.greenproject.classway.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 마이페이지 기능의 실제 처리(비즈니스 로직)를 담당하는 클래스
 * - 회원 정보 조회 (MY-001)
 * - 앞으로 회원 정보 수정, 탈퇴, 수강 내역, 알림 설정, 쿠폰·포인트 기능이 이 클래스에 추가됨
 */
@Service                // Spring이 이 클래스를 직접 만들어서 관리 (Controller에 자동으로 넣어줄 수 있게 됨)
@AllArgsConstructor     // 아래 final 필드를 모두 받는 생성자 자동 생성 → Spring이 필요한 객체를 넣어줌 (의존성 주입)
public class UserService {

    private final UserRepository userRepository;    // users 테이블 조회용
    private final JwtTokenProvider tokenProvider;   // 토큰에서 사용자 정보를 꺼내는 도구

    // ==================================================
    // 회원 정보 조회 (MY-001)
    // ==================================================
    /**
     * 로그인한 사용자의 회원 정보를 조회한다.
     * @param accessToken "Bearer "를 뗀 순수 엑세스 토큰
     * @return 마이페이지 회원정보 화면에 표시할 7개 값
     */
    @Transactional(readOnly = true)     // DB를 읽기만 하는 작업이라는 표시 (수정 없음)
    public MyProfileResponse getMyProfile(String accessToken) {

        // 1. 토큰으로 로그인한 사용자 찾기 (없으면 공통 메서드 안에서 404 에러 발생)
        User user = findLoginUser(accessToken);

        // 2. 화면에 필요한 값만 꺼내서 응답 DTO에 담기
        //    ※ 순서는 MyProfileResponse의 필드 선언 순서와 반드시 같아야 함
        //    ※ 비밀번호, 포인트, 계정 상태 등은 담지 않음 (화면에 불필요 + 보안)
        return new MyProfileResponse(
                user.getProfileImage(),         // 프로필 이미지 경로 (사진이 없으면 null)
                user.getEmail(),                // 이메일 (수정 불가)
                user.getName(),                 // 이름 (수정 불가)
                user.getNickname(),             // 닉네임
                user.getPhone(),                // 연락처
                user.getRole().name(),          // 역할 enum → 문자열 (예: "STUDENT")
                user.getProvider().name()       // 가입 방식 enum → 문자열 (예: "LOCAL")
        );
    }

    // ==================================================
    // 공통 메서드 (마이페이지 기능에서 함께 사용)
    // ==================================================
    /**
     * 엑세스 토큰으로 로그인한 사용자를 DB에서 찾는다.
     * 마이페이지의 모든 기능이 "누가 요청했는지" 확인할 때 이 메서드를 사용한다.
     * @param accessToken "Bearer "를 뗀 순수 엑세스 토큰
     * @return 로그인한 사용자(User 엔티티)
     * @throws ResourceNotFoundException DB에 해당 사용자가 없을 때 (404)
     */
    private User findLoginUser(String accessToken) {

        // 1. 토큰 안에 저장된 사용자 번호(user_id) 꺼내기
        int userId = tokenProvider.getUserIdFromToken(accessToken);

        // 2. 사용자 번호로 DB 조회 (결과가 있을 수도, 없을 수도 있어서 Optional로 받음)
        Optional<User> optionalUser = userRepository.findById(userId);

        // 3. 사용자가 없으면 404 에러
        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("존재하지 않는 계정입니다.");
        }

        // 4. Optional 상자에서 실제 User 꺼내서 돌려주기
        return optionalUser.get();
    }
}