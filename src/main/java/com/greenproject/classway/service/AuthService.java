package com.greenproject.classway.service;

import com.greenproject.classway.dto.*;
import com.greenproject.classway.entity.*;
import com.greenproject.classway.exceptions.*;
import com.greenproject.classway.global.FileStatus;
import com.greenproject.classway.global.UserState;
import com.greenproject.classway.repository.*;
import com.greenproject.classway.security.JwtTokenProvider;
import com.greenproject.classway.service.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklistRepository accessTokenBlackListRepository;
    private final TutorsCareerFileRepository tutorsCareerFileRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService; // 인터페이스 주입

    // 수강생 회원가입
    @Transactional
    public void signupStudent(SignupStudentRequest signupStudentRequest) {

        // 이메일이 사용 중인지 확인
        if (userRepository.existsByEmail(signupStudentRequest.getEmail())) {
            throw new ResourceConflictException("Email already exists");
        }

        // 닉네임이 사용 중인지 확인
        if (userRepository.existsByNickname(signupStudentRequest.getNickname())) {
            throw new ResourceConflictException("NickName already exists");
        }

        // 비밀번호 해싱
        String hashedPassword = passwordEncoder.encode(signupStudentRequest.getPassword());

        // db save
        User user = new User();
        user.setEmail(signupStudentRequest.getEmail());
        user.setPassword(hashedPassword);
        user.setName(signupStudentRequest.getName());
        user.setPhone(signupStudentRequest.getPhone());
        user.setNickname(signupStudentRequest.getNickname());

        // 1. 파일 저장 서비스 호출 (로컬이든 S3든 인터페이스를 통해 알아서 처리됨)
        List<String> imageFilePaths = fileStorageService.storeFiles(signupStudentRequest.getProfile_image());
        // 2. 반환받은 경로(URL) 리스트를 DB 엔티티에 매핑하여 저장
        for (String path : imageFilePaths) {
            user.setProfileImage(path);
        }

        user.setRole(signupStudentRequest.getRole());
        user.setClassContentAlarm(signupStudentRequest.getClass_content_alarm());
        user.setQnaAlarm(signupStudentRequest.getQna_alarm());
        user.setOfflineClassAlarm(signupStudentRequest.getOffline_class_alarm());
        user.setMarketingAgree(signupStudentRequest.getMarketing_agree());

        userRepository.save(user);
    }

    // 강사 회원가입
    @Transactional
    public void signupTutor(SignupTutorRequest signupTutorRequest) {

        // 이메일이 사용 중인지 확인
        if (userRepository.existsByEmail(signupTutorRequest.getEmail())) {
            throw new ResourceConflictException("Email already exists");
        }

        // 닉네임이 사용 중인지 확인
        if (userRepository.existsByNickname(signupTutorRequest.getNickname())) {
            throw new ResourceConflictException("NickName already exists");
        }

        // 비밀번호 해싱
        String hashedPassword = passwordEncoder.encode(signupTutorRequest.getPassword());

        // user table save
        User user = new User();
        user.setEmail(signupTutorRequest.getEmail());
        user.setPassword(hashedPassword);
        user.setName(signupTutorRequest.getName());
        user.setPhone(signupTutorRequest.getPhone());
        user.setNickname(signupTutorRequest.getNickname());

        // 1. 파일 저장 서비스 호출 (로컬이든 S3든 인터페이스를 통해 알아서 처리됨)
        List<String> imageFilePaths = fileStorageService.storeFiles(signupTutorRequest.getProfile_image());
        // 2. 반환받은 경로(URL) 리스트를 DB 엔티티에 매핑하여 저장
        for (String path : imageFilePaths) {
            user.setProfileImage(path);
        }

        user.setRole(signupTutorRequest.getRole());
        user.setClassContentAlarm(signupTutorRequest.getClass_content_alarm());
        user.setQnaAlarm(signupTutorRequest.getQna_alarm());
        user.setOfflineClassAlarm(signupTutorRequest.getOffline_class_alarm());
        user.setMarketingAgree(signupTutorRequest.getMarketing_agree());

        userRepository.save(user);

        // tutor table save
        Optional<User> optionalUser = userRepository.findByEmail(signupTutorRequest.getEmail());

        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("존재하지 않는 계정 입니다.");
        }

        User newUser = optionalUser.get();

        Tutor tutor = new Tutor();
        tutor.setUser(newUser);
        tutor.setIntro(signupTutorRequest.getIntro());
        tutor.setCareer(signupTutorRequest.getCareer());

        tutorRepository.save(tutor);

        // TutorsCareerFile table save
        Optional<Tutor> optionalTutor = tutorRepository.findByUserId(newUser.getId());

        if (optionalTutor.isEmpty()) {
            throw new ResourceNotFoundException("존재하지 않는 강사 입니다.");
        }

        Tutor newTutor = optionalTutor.get();

        List<String> documentFilePaths = fileStorageService.storeFiles(signupTutorRequest.getDocument_file());
        for (String path : documentFilePaths) {
            String normalizedPath = path.replace("\\", "/");
            String savedFileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);

            TutorsCareerFile tutorsCareerFile = new TutorsCareerFile();
            tutorsCareerFile.setTutor(newTutor);
            tutorsCareerFile.setFileName(savedFileName);
            tutorsCareerFile.setFileUrl(path);
            tutorsCareerFile.setStatus(FileStatus.TEMP);

            tutorsCareerFileRepository.save(tutorsCareerFile);
        }
    }

    // 로그인
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new UnauthenticatedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 1. 계정 존재하는건지 확인
        Optional<User> optionalUser = userRepository.findByEmail(loginRequest.getEmail());

        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("존재하지 않는 계정 입니다.");
        }

        User user = optionalUser.get();

        if (user.getStatus() == UserState.DISABLED) {
            throw new ResourceNotFoundException("탈퇴된 회원입니다.");
        }
        else if (user.getStatus() == UserState.WITHDRAWN) {
            throw new ResourceNotFoundException("휴면된 계정입니다.");
        }

        // 엑세스 토큰 발급
        String accessToken = tokenProvider.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        // 3-1. 해당 유저의 기존 리프레시 토큰(들) 삭제
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(user.getId());
        refreshTokenRepository.deleteAll(tokens);

        // 3-2. 해당 유저의 신규 리프레시 토큰 저장
        Date expiration = tokenProvider.getExpirationToken(refreshToken);
        LocalDateTime expirationDatetime = expiration.toInstant().atZone(
                ZoneId.systemDefault()
        ).toLocalDateTime();

        RefreshToken refreshTokenObj = new RefreshToken();
        refreshTokenObj.setUser(user);
        refreshTokenObj.setToken(refreshToken);
        refreshTokenObj.setExpirationDatetime(expirationDatetime);
        refreshTokenRepository.save(refreshTokenObj);

        // 4. 로그인 성공
        return new LoginResponse(accessToken, refreshToken, user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }

    // 로그아웃(토큰 삭제)
    @Transactional
    public void logout(String accessToken) {
        // 1. 해당 사용자의 리프레시 토큰 무효화
        // - DB에서 해당 사용자의 리프레시 토큰을 지운다.
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("존재하지 않는 계정 입니다.");
        }

        User user = optionalUser.get();

        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(user.getId());
        refreshTokenRepository.deleteAll(tokens);

        // 2. 해당 사용자의 엑세스 토큰 무효화
        // - DB 내 블랙리스트 테이블에 해당 토큰을 추가한다.
        // - 만료 이후에는 블랙리스트 해제가 필요하므로 만료일자도 함께 넣는다.
        Date expiration = tokenProvider.getExpirationToken(accessToken);
        LocalDateTime convertedDateTime = expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        AccessTokenBlacklist accessTokenBlackList = new AccessTokenBlacklist();
        accessTokenBlackList.setToken(accessToken);
        accessTokenBlackList.setExpirationDatetime(convertedDateTime);
        accessTokenBlackListRepository.save(accessTokenBlackList);
    }

    // 엑세스 토큰 재발급
    @Transactional(readOnly = true)
    public RefreshResponse refresh(@Valid RefreshRequest refreshRequest) {
        String userRefreshToken = refreshRequest.getRefreshToken();
        // 1. 리프레시 토큰 위/변조 여부 검증
        if (!tokenProvider.validateRefreshToken(userRefreshToken)) {
            // 검증 실패 예외처리
            throw new UnauthenticatedException("만료되었거나 유효하지 않는 리프레시 토큰입니다.");
        }

        // 2. DB 내 존재하는 리프레시 토큰인지 검증
        Optional<RefreshToken> optionalRefreshToken = refreshTokenRepository.findByToken(userRefreshToken);
        if (optionalRefreshToken.isEmpty()) {
            throw new UnauthenticatedException("유효하지 않는 리프레시 토큰입니다.");
        }

        // 3. 해당 리프레시 토큰이 만료되진 않았는지 검증
        // ! + isAfter = 만료 시간이 현재보다 이전이거나 같은 경우 (=현재보다 미래가 아닌 경우)
        // isBefore = 만료 시간이 현재보다 이전인 경우
        // 만료 시각과 현재 시각이 정확히 같을 때도 만료된 것으로 처리
        if (!optionalRefreshToken.get().getExpirationDatetime().isAfter(LocalDateTime.now())) {
            throw new UnauthenticatedException("만료된 리프레시 토큰입니다.");
        }

        // 4. 토큰 내 유저네임(subject)의 유저 정보가 유효한지 검증
        // 해당 유저가 존재하는지, 활성화 상태인지(탈퇴하지는 않았는지) 등등
        String email = tokenProvider.getUsernameFromToken(userRefreshToken);
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("존재하지 않는 계정 입니다.");
        }

        User user = optionalUser.get();

        if (user == null ||  user.getStatus() != UserState.ACTIVE) {
            throw new UnauthenticatedException("인증할 수 없는 계정입니다.");
        }

        // 신규 엑세스 토큰 생성
        String accessToken = tokenProvider.generateAccessToken(email, user.getId(), user.getRole().name());
        return new RefreshResponse(accessToken);
    }

    // 접속자 정보 조회
    @Transactional(readOnly = true)
    public MyInfoResponse getUserInfo(String accessToken) {
        // 1. 유저 아이디로 DB 조회함
        int userId = tokenProvider.getUserIdFromToken(accessToken);
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new ResourceNotFoundException("존재하지 않는 계정입니다.");
        }

        User user = optionalUser.get();

        // 2. 유저 ID로 DB에서 이 유저의 이름과 이메일을 받아옴.
        String name = user.getName();
        String email = user.getEmail();

        // 3. MyInfoResponse 객체를 생성하여 돌려줌
        return new MyInfoResponse(userId, email, name, user.getRole().name());

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
