package com.greenproject.classway.service;

import com.greenproject.classway.dto.AdminTutorCountResponse;
import com.greenproject.classway.dto.AdminTutorDetailResponse;
import com.greenproject.classway.dto.AdminTutorListResponse;
import com.greenproject.classway.dto.TutorFileDownload;
import com.greenproject.classway.dto.TutorFileResponse;
import com.greenproject.classway.dto.TutorRejectRequest;
import com.greenproject.classway.entity.Tutor;
import com.greenproject.classway.entity.TutorsCareerFile;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.AuthorizationFailureException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.exceptions.InvalidStateException;
import com.greenproject.classway.global.AlarmType;
import com.greenproject.classway.global.TutorApprovalStatus;
import com.greenproject.classway.global.UserRole;
import com.greenproject.classway.repository.TutorRepository;
import com.greenproject.classway.repository.TutorsCareerFileRepository;
import com.greenproject.classway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 페이지 기능의 실제 처리(비즈니스 로직)를 담당하는 클래스
 * - 강사 승인 화면 (ADMIN-002): 신청 목록·탭별 건수·상세·제출 자료 보기, 승인·반려, 처리 결과 알림
 * - 모든 메서드는 첫 줄에서 findLoginAdmin()으로 관리자인지 검사 (아니면 403)
 */
@Service
public class AdminService {
    private final UserRepository userRepository;    // 로그인한 사용자가 관리자인지 확인용
    private final TutorRepository tutorRepository;  // 강사 신청서 조회·수정용
    private final TutorsCareerFileRepository tutorsCareerFileRepository; // 제출 자료 파일 조회용
    private final AlarmService alarmService;        // 승인·반려 결과를 신청자에게 알림
    private final Path uploadRoot;                  // 업로드 폴더 (제출 자료 보기 때 이 폴더 밖 파일은 막음)

    // @Value는 Lombok 생성자로 넘길 수 없어서 생성자를 직접 작성
    public AdminService(UserRepository userRepository,
                        TutorRepository tutorRepository,
                        TutorsCareerFileRepository tutorsCareerFileRepository,
                        AlarmService alarmService,
                        @Value("${spring.file.storage}") String uploadDir) {
        this.userRepository = userRepository;
        this.tutorRepository = tutorRepository;
        this.tutorsCareerFileRepository = tutorsCareerFileRepository;
        this.alarmService = alarmService;
        // 파일 저장(LocalFileStorageServiceImpl)과 같은 설정값 → 절대 경로로 바꿔 둠
        // - 실제 경로(toRealPath) 변환은 파일 보기 때 함 (서버 시작 시 폴더가 아직 없으면 여기서 예외가 나 서버가 안 뜰 수 있어서)
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

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

        // 2. 해당 상태의 신청서들을 신청자(users)와 함께 DB에서 꺼냄. (신청일 오래된 순으로)
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
    // 강사 신청 탭별 건수 (ADMIN-002)
    //====================================
    /**
     * 승인 대기·승인·반려 탭에 표시할 건수 조회
     * @return 상태별 건수 (0건인 상태도 0으로 채움)
     */
    @Transactional(readOnly = true)
    public AdminTutorCountResponse getTutorApplicationCounts() {
        // 1. 관리자인지 확인 (아니면 403)
        findLoginAdmin();

        // 2. 상태별 건수 조회 → 결과 한 줄 = [상태, 건수]
        long pending = 0;
        long approved = 0;
        long rejected = 0;
        for (Object[] row : tutorRepository.countGroupByApprovalStatus()) {
            TutorApprovalStatus status = (TutorApprovalStatus) row[0];
            long count = (Long) row[1];
            switch (status) {
                case PENDING -> pending = count;
                case APPROVED -> approved = count;
                case REJECTED -> rejected = count;
            }
        }
        // 3. 결과에 없던 상태는 0 그대로
        return new AdminTutorCountResponse(pending, approved, rejected);
    }
    //====================================
    // 강사 신청 상세 조회 (ADMIN-002)
    //====================================
    /**
     * 강사 신청 한 건의 상세 정보 조회 (신청자 정보 + 경력 + 제출 자료 + 처리 결과)
     * - 승인 대기·승인·반려 어느 탭에서 눌러도 볼 수 있어야 함
     *   → 대기 중인지 검사하는 findPendingTutor(400) 대신, 있는지만 검사하는 findTutor(404) 사용
     * @param tutorId 조회할 신청 번호
     * @return 신청 상세 정보
     */
    @Transactional(readOnly = true)
    public AdminTutorDetailResponse getTutorApplicationDetail(Integer tutorId) {
        // 1. 관리자인지 확인 (아니면 403)
        findLoginAdmin();

        // 2. 신청서 찾기 (없으면 404)
        Tutor tutor = findTutor(tutorId);
        User applicant = tutor.getUser(); // 신청자 회원 정보 (users)

        // 3. 제출 자료 파일들 꺼내서(올린 순서대로) 응답용 상자로 옮겨 담기
        //    - 가입 시 파일이 TEMP로만 저장되고 있어서 status로 거르지 않음
        List<TutorsCareerFile> careerFiles = tutorsCareerFileRepository.findByTutorOrderByFileIdAsc(tutor);
        List<TutorFileResponse> files = new ArrayList<>();
        for (TutorsCareerFile file : careerFiles) {
            files.add(new TutorFileResponse(file.getFileId(), toOriginalFileName(file.getFileName())));
        }

        // 4. 상세 응답 만들기 (DTO 필드 순서와 같게)
        return new AdminTutorDetailResponse(
                tutor.getTutorId(),                  // 신청 번호
                applicant.getName(),                 // 이름
                applicant.getEmail(),                // 이메일
                applicant.getPhone(),                // 연락처
                tutor.getAppliedAt(),                // 신청일
                tutor.getCareer(),                   // 경력 (가입 때 선택 입력이라 null 가능)
                files,                               // 제출 자료 목록
                tutor.getApprovalStatus().name(),    // 상태 (enum → 문자열)
                tutor.getRejectReason(),             // 반려 사유 (없으면 null)
                tutor.getApprovedAt()                // 처리 시간 (없으면 null)
        );
    }
    //====================================
    // 강사 제출 자료 보기 (ADMIN-002)
    //====================================
    /**
     * 강사 신청서에 첨부된 제출 자료 파일 1개를 꺼낸다
     * @param tutorId 신청 번호
     * @param fileId  파일 번호 (상세 조회의 files[].fileId)
     * @return 파일 + Content-Type + 원래 파일명
     * @throws ResourceNotFoundException     파일 정보가 없거나 다른 신청서 파일일 때, 디스크에 파일이 없을 때 (404)
     * @throws AuthorizationFailureException 파일의 실제 위치(바로가기를 따라간 경로)가 업로드 폴더 밖일 때 (403)
     */
    @Transactional(readOnly = true)
    public TutorFileDownload getTutorFile(Integer tutorId, Integer fileId) {
        // 1. 관리자인지 확인 (아니면 403)
        findLoginAdmin();

        // 2. 파일 번호 + 신청 번호가 둘 다 맞는 파일 정보 찾기 (없으면 404)
        TutorsCareerFile file = tutorsCareerFileRepository.findByFileIdAndTutor_TutorId(fileId, tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 파일입니다."));

        // 3. 바로가기(심볼릭 링크)를 끝까지 따라간 "실제 경로"로 변환
        // - 업로드 폴더도 실제 경로로 구해야 두 경로를 같은 기준으로 비교할 수 있음
        // - 파일(또는 업로드 폴더)이 실제로 없으면 toRealPath()가 IOException -> 404
        Path realRoot;
        Path realPath;
        try {
            realRoot = uploadRoot.toRealPath();
            realPath = Paths.get(file.getFileUrl()).toRealPath();
        } catch (IOException e) {
            throw new ResourceNotFoundException("파일을 찾을 수 없습니다.");
        }

        // 4. 실제 위치가 업로드 폴더 밖이면 막음 (바로가기로 다른 파일을 읽는 것 방지)
        if (!realPath.startsWith(realRoot)) {
            throw new AuthorizationFailureException("접근할 수 없는 파일입니다.");
        }

        // 5. 폴더 등 일반 파일이 아니면 404 (존재 여부는 3단계에서 이미 확인)
        if (!Files.isRegularFile(realPath)) {
            throw new ResourceNotFoundException("파일을 찾을 수 없습니다.");
        }

        // 6. 파일 종류 판별 (모르면 일반 바이너리)
        String contentType = null;
        try {
            contentType = Files.probeContentType(realPath);
        } catch (IOException ignored) {
            // 판별 실패 → 아래에서 기본값 사용
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // 7. 검사를 통과한 실제 경로의 파일 + 종류 + 원래 파일명 돌려주기
        return new TutorFileDownload(
                new FileSystemResource(realPath),
                contentType,
                toOriginalFileName(file.getFileName())
        );
    }
    //====================================
    // 강사 승인 (ADMIN-002)
    //====================================
    /**
     * 승인 대기 중인 강사 신청을 승인한다.
     * - role은 가입 때 이미 TUTOR라서 바꾸지 않음
     * - 처리 후 신청자에게 승인 알림(CONFIRM)을 보냄
     * @param tutorId 승인할 신청 번호
     */
    @Transactional
    public void approveTutor(Integer tutorId) {
        // 1. 관리자인지 확인 (아니면 403) -> tutors 테이블에 처리한 관리자 기록용
        User admin = findLoginAdmin();

        // 2. 신청서가 있는지 (404), 아직 승인 대기인지(400) 확인
        Tutor tutor = findPendingTutor(tutorId);
        User applicant = tutor.getUser(); // 알림 받을 신청자

        // 3. 승인 처리 - PENDING일 때만 바뀌는 UPDATE (상태, 처리한 관리자, 처리 시간)
        //    0건 = 2번 확인 뒤 다른 관리자가 먼저 처리함 → 400
        int updated = tutorRepository.processPendingApplication(
                tutorId, TutorApprovalStatus.APPROVED, null, admin, LocalDateTime.now());
        if (updated == 0) {
            throw new InvalidStateException("이미 처리된 강사 신청입니다.");
        }

        // 4. 신청자에게 승인 알림 (같은 트랜잭션 → 알림 저장이 실패하면 승인도 취소)
        // TODO: 알림 클릭 시 이동할 주소(linkUrl)는 프론트와 협의 후 지정
        alarmService.send(applicant, AlarmType.CONFIRM, "강사 신청이 승인되었습니다.", null);
    }
    //====================================
    // 강사 반려 (ADMIN-002)
    //====================================
    /**
     * 승인 대기 중인 강사 신청을 반려한다
     * - 반려 사유가 비었는지는 Controller의 @Valid가 먼저 검사함(400)
     * - approved_by, approved_at의 이름은 "승인"이지만 반려한 관리자·시간도 함께 기록하는 칸임
     * - 처리 후 신청자에게 반려 알림(CONFIRM, 사유 포함)을 보냄
     * @param tutorId 반려할 신청 번호
     * @param request 반려 사유
     */
    @Transactional
    public void rejectTutor(Integer tutorId, TutorRejectRequest request) {
        // 1. 관리자인지 확인 (아니면 403) -> 해당 작업 처리한 관리자 기록용
        User admin = findLoginAdmin();

        // 2. 신청서가 있는지(404), 아직 승인 대기인지(400) 확인 -> 승인과 같은 공통 메서드 재사용
        Tutor tutor = findPendingTutor(tutorId);
        User applicant = tutor.getUser(); // 알림 받을 신청자

        // 3. 반려 처리 - PENDING일 때만 바뀌는 UPDATE (상태, 반려 사유, 처리한 관리자, 처리 시간)
        //    0건 = 다른 관리자가 먼저 처리함 → 400
        int updated = tutorRepository.processPendingApplication(
                tutorId, TutorApprovalStatus.REJECTED, request.getRejectReason(), admin, LocalDateTime.now());
        if (updated == 0) {
            throw new InvalidStateException("이미 처리된 강사 신청입니다.");
        }

        // 4. 신청자에게 반려 알림 (사유 포함, 100자 넘으면 AlarmService가 자름)
        // TODO: 알림 클릭 시 이동할 주소(linkUrl)는 프론트와 협의 후 지정
        alarmService.send(applicant, AlarmType.CONFIRM,
                "강사 신청이 반려되었습니다. 사유: " + request.getRejectReason(), null);
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
    /**
     * 저장 파일명에서 원래 파일명만 꺼낸다 (상세 조회·파일 보기 공통)
     * - 저장 이름이 "uuid(36자) + _ + 원래 파일명" 형태일 때만 앞 37자를 뗌
     * - 형태가 다르면 자르지 않고 그대로 → 업로드 방식이 바뀌어도 글자가 잘려 나가지 않음
     */
    private String toOriginalFileName(String savedName) {
        return (savedName.length() > 37 && savedName.charAt(36) == '_')
                ? savedName.substring(37)
                : savedName;
    }
}
