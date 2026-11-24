package com.greenproject.classway.service;

import com.greenproject.classway.dto.TutorClassListResponse;
import com.greenproject.classway.dto.TutorClassListResponse.*;
import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.entity.Tutor;
import com.greenproject.classway.exceptions.AuthorizationFailureException;
import com.greenproject.classway.exceptions.InvalidStateException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import com.greenproject.classway.global.ReservationStatus;
import com.greenproject.classway.global.TutorApprovalStatus;
import com.greenproject.classway.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TutorClassService {

    private final TutorRepository tutorRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final LectureRepository lectureRepository;
    private final ReservationRepository reservationRepository;

    private static final int DEFAULT_PAGE_SIZE = 6; // 시안 기준 (3열 × 2줄)
    private static final int MAX_PAGE_SIZE = 20;    // 한 번에 조회 가능한 최대 크기 (상한선)

    /**
     * 1. 강사 등록 클래스 목록 조회
     */
    @Transactional(readOnly = true)
    public TutorClassListResponse getTutorClasses(String loginEmail, int page, int size) {
        // [1단계] 강사 자격 검증 (승인된 강사인지 확인)
        Tutor tutor = validateAndGetTutor(loginEmail);

        // [2단계] 강사가 등록한 클래스 페이징 조회 (최신 등록순)
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "classId"));

        Page<ClassEntity> classPage = classEntityRepository.findByTutor_TutorId(tutor.getTutorId(), pageable);

        // 등록된 클래스가 0건이면 빈 목록을 정상 반환
        if (classPage.isEmpty()) {
            return TutorClassListResponse.builder()
                    .totalCount(classPage.getTotalElements())   // 실제 전체 건수 (예: 14건) 유지
                    .totalPages(classPage.getTotalPages())      // 실제 전체 페이지 수 (예: 3페이지) 유지
                    .currentPage(safePage)
                    .classes(Collections.emptyList())           // 데이터 목록만 빈 리스트 반환
                    .build();
        }

        // 이번 페이지에 보여줄 클래스 목록 및 ID 목록 추출
        List<ClassEntity> classList = classPage.getContent();
        List<Integer> classIds = new ArrayList<>();
        for (ClassEntity clazz : classList) {
            classIds.add(clazz.getClassId());
        }

        // [3단계] 집계 데이터 조회 (IN절 배치 조회) 및 Map(사물함)에 담기

        // 3-1. 오프라인 스케줄 집계: Map<클래스ID, 스케줄요약정보>
        Map<Integer, ClassScheduleRepository.OfflineSummaryProjection> offlineSummaryMap = new HashMap<>();
        List<ClassScheduleRepository.OfflineSummaryProjection> scheduleSummaries =
                classScheduleRepository.summarizeSchedulesByClassIds(classIds);
        for (ClassScheduleRepository.OfflineSummaryProjection summary : scheduleSummaries) {
            offlineSummaryMap.put(summary.getClassId(), summary);
        }

        // 3-2. 온라인 VOD 강의 수 집계: Map<클래스ID, VOD개수>
        Map<Integer, Long> lectureCountMap = new HashMap<>();
        List<LectureRepository.LectureCountProjection> lectureCounts =
                lectureRepository.countLecturesByClassIds(classIds);
        for (LectureRepository.LectureCountProjection countRow : lectureCounts) {
            lectureCountMap.put(countRow.getClassId(), countRow.getLectureCount());
        }

        // 3-3. 온라인 수강생 수 집계: Map<클래스ID, 수강생수>
        Map<Integer, Long> studentCountMap = new HashMap<>();
        List<ReservationRepository.ReservationCountProjection> studentCounts =
                reservationRepository.countReservationsByClassIds(
                        classIds,
                        List.of(ReservationStatus.RESERVED, ReservationStatus.COMPLETED)
                );
        for (ReservationRepository.ReservationCountProjection countRow : studentCounts) {
            studentCountMap.put(countRow.getClassId(), countRow.getStudentCount());
        }

        // [4단계] 시안 UI 카드 DTO 리스트 조립
        List<ClassItem> cardItems = new ArrayList<>();
        for (ClassEntity clazz : classList) {
            Integer classId = clazz.getClassId();
            ClassStatus status = clazz.getStatus();

            // 토글 스위치 조작 가능 여부: 오픈(PUBLISHED) 또는 비활성(HIDDEN)일 때만 가능
            // 검토 대기(PENDING)나 판매 중지(STOPPED)는 false
            boolean canToggle = (status == ClassStatus.PUBLISHED || status == ClassStatus.HIDDEN);

            OfflineInfo offlineInfo = null;
            OnlineInfo onlineInfo = null;

            // 오프라인인 경우: 아까 Map에 담아둔 데이터 꺼내기
            if (clazz.getClassType() == ClassType.OFFLINE) {
                var summary = offlineSummaryMap.get(classId);
                long scheduleCount = (summary != null) ? summary.getScheduleCount() : 0L;
                long reservedCount = (summary != null) ? summary.getTotalReserved() : 0L;
                long capacity      = (summary != null) ? summary.getTotalCapacity() : 0L;

                offlineInfo = OfflineInfo.builder()
                        .scheduleCount(scheduleCount)
                        .reservedCount(reservedCount)
                        .capacity(capacity)
                        .build();
            }
            // 온라인인 경우: 아까 Map에 담아둔 데이터 꺼내기
            else if (clazz.getClassType() == ClassType.ONLINE) {
                long lectureCount = lectureCountMap.getOrDefault(classId, 0L);
                long studentCount = studentCountMap.getOrDefault(classId, 0L);

                onlineInfo = OnlineInfo.builder()
                        .lectureCount(lectureCount)
                        .studentCount(studentCount)
                        .build();
            }

            // 개별 카드 아이템 생성 후 리스트에 추가
            ClassItem item = ClassItem.builder()
                    .classId(clazz.getClassId())
                    .classType(clazz.getClassType())
                    .status(clazz.getStatus())
                    .title(clazz.getTitle())
                    .thumbnailUrl(clazz.getThumbnailUrl())
                    .canToggle(canToggle)
                    .offlineInfo(offlineInfo)
                    .onlineInfo(onlineInfo)
                    .build();

            cardItems.add(item);
        }

        // 최종 응답 반환
        return TutorClassListResponse.builder()
                .totalCount(classPage.getTotalElements())
                .totalPages(classPage.getTotalPages())
                .currentPage(safePage)
                .classes(cardItems)
                .build();
    }

    /**
     * 2. 클래스 상태 변경 (PUBLISHED <-> HIDDEN)
     */
    @Transactional
    public void updateClassStatus(Integer classId, ClassStatus requestedStatus, String loginEmail) {
        // [1단계] 강사 식별 및 자격 검증
        Tutor tutor = validateAndGetTutor(loginEmail);

        // [2단계] 요청 상태값 유효성 검증 (오직 PUBLISHED 또는 HIDDEN만 허용)
        if (requestedStatus != ClassStatus.PUBLISHED && requestedStatus != ClassStatus.HIDDEN) {
            throw new InvalidStateException("오픈(PUBLISHED) 또는 비활성(HIDDEN) 상태로만 변경할 수 있습니다.");
        }

        // [3단계] 클래스 조회 및 본인 소유 강의인지 검증
        ClassEntity clazz = classEntityRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 강의입니다."));

        if (!clazz.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new AuthorizationFailureException("본인이 등록한 강의만 상태를 변경할 수 있습니다.");
        }

        // [4단계] 현재 클래스 상태 기준 유효성 검증
        ClassStatus currentStatus = clazz.getStatus();

        if (currentStatus == ClassStatus.PENDING) {
            throw new InvalidStateException("관리자 검토가 진행 중인 강의는 상태를 변경할 수 없습니다.");
        }

        if (currentStatus == ClassStatus.STOPPED) {
            throw new AuthorizationFailureException("관리자에 의해 판매 중지된 강의는 상태를 변경할 수 없습니다. 1:1 문의를 이용해주세요.");
        }

        // 이미 같은 상태라면 추가 작업 없이 종료
        if (currentStatus == requestedStatus) {
            return;
        }

        // [5단계] 상태 변경 및 명시적 DB 저장
        clazz.setStatus(requestedStatus);
        classEntityRepository.save(clazz);
    }

    /**
     * 공통: 승인된 강사 검증 헬퍼 메서드
     */
    private Tutor validateAndGetTutor(String email) {
        if (email == null) {
            throw new AuthorizationFailureException("로그인이 필요한 서비스입니다.");
        }

        Tutor tutor = tutorRepository.findByUser_Email(email)
                .orElseThrow(() -> new AuthorizationFailureException("강사로 등록된 회원이 아닙니다."));

        if (tutor.getApprovalStatus() != TutorApprovalStatus.APPROVED) {
            throw new AuthorizationFailureException("승인 완료된 강사만 이용할 수 있습니다.");
        }

        return tutor;
    }
}
