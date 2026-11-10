package com.greenproject.classway.service;

import com.greenproject.classway.repository.AccessTokenBlacklistRepository;
import com.greenproject.classway.repository.RefreshTokenRepository;
import com.greenproject.classway.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklistRepository accessTokenBlackListRepository;

    // 회원가입
    @Transactional
    public void signup() {
        // 이메일과 비밀번호가 공백이 아닌지 확인

        // username이 사용 중인지 확인

        // 이메일이 사용 중인지 확인


        // 비밀번호 해싱

        // db save
    }

    // 로그인
    @Transactional
    public void login() {
        try {

        } catch (Exception e) {

        }

        // 1.이메일과 비밀번호가 공백이 아닌지 확인

        // 2. 이메일 존재하는건지 확인

        // 엑세스 토큰 발급

        // 3-1. 해당 유저의 기존 리프레시 토큰(들) 삭제

        // 3-2. 해당 유저의 신규 리프레시 토큰 저장

        // 4. 로그인 성공

    }

    // 로그아웃(토큰 삭제)
    @Transactional
    public void logout(String accessToken) {
        // 1. 해당 사용자의 리프레시 토큰 무효화
        // - DB에서 해당 사용자의 리프레시 토큰을 지운다.

        // 2. 해당 사용자의 엑세스 토큰 무효화
        // - DB 내 블랙리스트 테이블에 해당 토큰을 추가한다.
        // - 만료 이후에는 블랙리스트 해제가 필요하므로 만료일자도 함께 넣는다.

    }

    // 엑세스 토큰 재발급
    @Transactional(readOnly = true)
    public void refresh() {
        // 1. 리프레시 토큰 위/변조 여부 검증

        // 2. DB 내 존재하는 리프레시 토큰인지 검증

        // 3. 해당 리프레시 토큰이 만료되진 않았는지 검증
        // ! + isAfter = 만료 시간이 현재보다 이전이거나 같은 경우 (=현재보다 미래가 아닌 경우)
        // isBefore = 만료 시간이 현재보다 이전인 경우
        // 만료 시각과 현재 시각이 정확히 같을 때도 만료된 것으로 처리

        // 4. 토큰 내 유저네임(subject)의 유저 정보가 유효한지 검증
        // 해당 유저가 존재하는지, 활성화 상태인지(탈퇴하지는 않았는지) 등등

        // 신규 엑세스 토큰 생성

    }

    // 접속자 정보 조회
    @Transactional(readOnly = true)
    public void getUserInfo() {
        // 1. 유저 아이디로 DB 조회함

        // 2. DB에서 이 유저의 닉네임과 이메일을 받아옴.
        // 3. MyInfoResponse 객체를 생성하여 돌려줌

    }

    // 회원 정보 수정
    @Transactional
    public void updateUserInfo() {

    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser() {

    }
}
