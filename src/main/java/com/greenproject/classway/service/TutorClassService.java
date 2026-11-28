package com.greenproject.classway.service;

import com.greenproject.classway.dto.*;
import com.greenproject.classway.dto.TutorClassListResponse.*;
import com.greenproject.classway.entity.*;
import com.greenproject.classway.exceptions.AuthorizationFailureException;
import com.greenproject.classway.exceptions.InvalidStateException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import com.greenproject.classway.global.ReservationStatus;
import com.greenproject.classway.global.TutorApprovalStatus;
import com.greenproject.classway.repository.*;
import com.greenproject.classway.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TutorClassService {

    private final TutorRepository tutorRepository;
    private final CategoryRepository categoryRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ClassChapterRepository classChapterRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final LectureRepository lectureRepository;
    private final ClassImageFileRepository classImageFileRepository;
    private final ReservationRepository reservationRepository;
    private final FileStorageService fileStorageService;

    private static final int DEFAULT_PAGE_SIZE = 6; // 시안 기준 (3열 × 2줄)
    private static final int MAX_PAGE_SIZE = 20;    // 한 번에 조회 가능한 최대 크기 (상한선)
    private static final long MAX_BANNER_FILE_SIZE = 5 * 1024 * 1024; // 배너 이미지 최대 5MB
    private static final int MAX_FILE_NAME_LENGTH = 100;
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png");

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
     * 3. 클래스 등록 (POST /tutor/class_create)
     */
    @Transactional
    public Integer createClass(TutorClassCreateRequest request, String loginEmail) {
        // [1단계] 강사 자격 검증
        Tutor tutor = validateAndGetTutor(loginEmail);

        // [2단계] 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 카테고리입니다."));

        // [3단계] 배너 이미지 필수 및 형식(JPG, PNG, 5MB 이하) 검증
        MultipartFile bannerFile = request.getBannerImage();
        if (bannerFile == null || bannerFile.isEmpty()) {
            throw new InvalidStateException("배너 이미지 파일은 필수입니다.");
        }
        validateBannerImage(bannerFile);

        // [4단계] 진행 방식에 따른 수업 장소(address) 및 지역(region) 처리
        String address = null;
        String region = null;
        if (request.getClassType() == ClassType.OFFLINE) {
            if (request.getAddress() == null || request.getAddress().isBlank()) {
                throw new InvalidStateException("오프라인 클래스는 수업 장소가 필수입니다.");
            }
            address = request.getAddress().trim();
            region = extractRegion(address);
        }

        // [5단계] 할인율 검증 및 10원 단위 절사 할인가(salePrice) 계산
        Integer salePrice = calculateSalePrice(request.getPrice(), request.getDiscountRate());

        // [6단계] 커리큘럼 검증 (챕터 1개 이상, 모든 챕터에 최소 1개 이상의 강의 필수)
        validateCreateCurriculum(request.getChapters(), request.getLectures());

        // [7단계] 배너 이미지 저장
        List<String> storedPaths = fileStorageService.storeFiles(List.of(bannerFile));
        String thumbnailUrl = storedPaths.isEmpty() ? null : storedPaths.get(0);

        // [8단계] 클래스 엔티티 생성 및 저장 (초기 상태: DRAFT)
        ClassEntity clazz = new ClassEntity();
        clazz.setTutor(tutor);
        clazz.setCategory(category);
        clazz.setTitle(request.getTitle().trim());
        clazz.setSummary(request.getSummary().trim());
        clazz.setDescription(request.getDescription());
        clazz.setClassType(request.getClassType());
        clazz.setPrice(request.getPrice());
        clazz.setSalePrice(salePrice);
        clazz.setThumbnailUrl(thumbnailUrl);
        clazz.setAddress(address);
        clazz.setRegion(region);
        clazz.setStatus(ClassStatus.DRAFT);

        ClassEntity savedClass = classEntityRepository.save(clazz);

        // [9단계] 챕터 저장 (클래스 내 chapter_id 1부터 순차 부여) 및 sortOrder 기준 매핑
        Map<Integer, Integer> sortOrderToChapterIdMap = new HashMap<>();
        int nextChapterId = 1;
        for (TutorClassCreateRequest.ChapterCreateItem chapterReq : request.getChapters()) {
            int assignedChapterId = nextChapterId++;

            ClassChapter chapter = new ClassChapter();
            chapter.setClazz(savedClass);
            chapter.setChapterId(assignedChapterId);
            chapter.setTitle(chapterReq.getTitle().trim());
            chapter.setSortOrder(chapterReq.getSortOrder());
            classChapterRepository.save(chapter);

            sortOrderToChapterIdMap.put(chapterReq.getSortOrder(), assignedChapterId);
        }

        // [10단계] 강의 저장 (소속된 챕터의 정렬 순서로 chapterId 매핑, 영상 길이는 초 단위 그대로 저장)
        List<Lecture> lecturesToSave = new ArrayList<>();
        for (TutorClassCreateRequest.LectureCreateItem lectureReq : request.getLectures()) {
            Integer targetChapterId = sortOrderToChapterIdMap.get(lectureReq.getChapterSortOrder());
            if (targetChapterId == null) {
                throw new InvalidStateException("강의가 소속된 챕터의 정렬 순서가 올바르지 않습니다.");
            }

            Lecture lecture = new Lecture();
            lecture.setClassId(savedClass.getClassId());
            lecture.setChapterId(targetChapterId);
            lecture.setTitle(lectureReq.getTitle().trim());
            lecture.setDurationSec(lectureReq.getDurationSec());
            lecture.setSortOrder(lectureReq.getSortOrder());
            lecture.setIsPreview(false);
            lecturesToSave.add(lecture);
        }
        lectureRepository.saveAll(lecturesToSave);

        return savedClass.getClassId();
    }

    /**
     * 4. 수정할 클래스 데이터 조회 (GET /tutor/{classId})
     */
    @Transactional(readOnly = true)
    public TutorClassDetailResponse getTutorClassDetail(Integer classId, String loginEmail) {
        Tutor tutor = validateAndGetTutor(loginEmail);
        ClassEntity clazz = findAndValidateOwnedClass(classId, tutor);

        Integer discountRate = calculateDiscountRate(clazz.getPrice(), clazz.getSalePrice());

        return TutorClassDetailResponse.builder()
                .classId(clazz.getClassId())
                .classType(clazz.getClassType())
                .status(clazz.getStatus())
                .title(clazz.getTitle())
                .categoryId(clazz.getCategory().getCategoryId())
                .summary(clazz.getSummary())
                .price(clazz.getPrice())
                .discountRate(discountRate)
                .salePrice(clazz.getSalePrice())
                .thumbnailUrl(clazz.getThumbnailUrl())
                .description(clazz.getDescription())
                .address(clazz.getAddress())
                .build();
    }

    /**
     * 5. 커리큘럼 조회 (GET /tutor/{classId}/chapter)
     */
    @Transactional(readOnly = true)
    public TutorCurriculumResponse getClassCurriculum(Integer classId, String loginEmail) {
        Tutor tutor = validateAndGetTutor(loginEmail);
        findAndValidateOwnedClass(classId, tutor);

        List<ClassChapter> chapters = classChapterRepository.findByClazz_ClassIdOrderBySortOrderAsc(classId);
        List<Lecture> lectures = lectureRepository.findByClassIdOrderBySortOrderAsc(classId);

        List<TutorCurriculumResponse.ChapterDto> chapterDtos = new ArrayList<>();
        for (ClassChapter chapter : chapters) {
            chapterDtos.add(TutorCurriculumResponse.ChapterDto.builder()
                    .chapterId(chapter.getChapterId())
                    .title(chapter.getTitle())
                    .sortOrder(chapter.getSortOrder())
                    .build());
        }

        List<TutorCurriculumResponse.LectureDto> lectureDtos = new ArrayList<>();
        for (Lecture lecture : lectures) {
            int durationSec = (lecture.getDurationSec() != null) ? lecture.getDurationSec() : 0;

            lectureDtos.add(TutorCurriculumResponse.LectureDto.builder()
                    .lectureId(lecture.getLectureId())
                    .chapterId(lecture.getChapterId())
                    .title(lecture.getTitle())
                    .durationSec(durationSec)
                    .sortOrder(lecture.getSortOrder())
                    .build());
        }

        return TutorCurriculumResponse.builder()
                .chapters(chapterDtos)
                .lectures(lectureDtos)
                .build();
    }

    /**
     * 6. 커리큘럼 저장 (PATCH /tutor/{classId}/chapter)
     */
    @Transactional
    public void saveClassCurriculum(Integer classId, TutorCurriculumSaveRequest request, String loginEmail) {
        // [1단계] 강사 및 클래스 소유권 확인
        Tutor tutor = validateAndGetTutor(loginEmail);
        ClassEntity clazz = findAndValidateOwnedClass(classId, tutor);

        // [2단계] 수정 차단 상태(PENDING, STOPPED) 확인
        validateEditableClassStatus(clazz.getStatus());

        // [3단계] 커리큘럼 입력값 검증 (빈 챕터 존재 여부 확인)
        validateSaveCurriculum(request.getChapters(), request.getLectures());

        // [4단계] 기존 DB 챕터 및 강의 조회
        List<ClassChapter> existingChapters = classChapterRepository.findByClazz_ClassIdOrderBySortOrderAsc(classId);
        List<Lecture> existingLectures = lectureRepository.findByClassIdOrderBySortOrderAsc(classId);

        // 요청에 남아있는 기존 챕터 번호(chapterId) 수집
        Set<Integer> incomingChapterIds = new HashSet<>();
        for (TutorCurriculumSaveRequest.ChapterSaveItem chReq : request.getChapters()) {
            if (chReq.getChapterId() != null) {
                incomingChapterIds.add(chReq.getChapterId());
            }
        }

        // 삭제 대상 챕터 번호 추출 (기존 DB에는 있으나 요청 데이터에는 없는 챕터 번호)
        List<Integer> deletedChapterIds = new ArrayList<>();
        for (ClassChapter existingCh : existingChapters) {
            if (!incomingChapterIds.contains(existingCh.getChapterId())) {
                deletedChapterIds.add(existingCh.getChapterId());
            }
        }

        // [5단계] 삭제된 챕터에 속한 강의 먼저 삭제 후 챕터 삭제 (예: 챕터 1,2,3 중 1,3만 넘어오면 2번 강의 삭제 -> 2번 챕터 삭제)
        if (!deletedChapterIds.isEmpty()) {
            List<Integer> lectureIdsInDeletedChapters = new ArrayList<>();
            for (Lecture existingLec : existingLectures) {
                if (deletedChapterIds.contains(existingLec.getChapterId())) {
                    lectureIdsInDeletedChapters.add(existingLec.getLectureId());
                }
            }
            if (!lectureIdsInDeletedChapters.isEmpty()) {
                classImageFileRepository.deleteByLectureIdIn(lectureIdsInDeletedChapters);
            }
            lectureRepository.deleteByClassIdAndChapterIdIn(classId, deletedChapterIds);
            classChapterRepository.deleteByClassIdAndChapterIdIn(classId, deletedChapterIds);
        }

        // 유지되는 챕터 내에서 개별 삭제된 강의 정리 (요청의 lectureId 목록에 없는 기존 강의 삭제)
        Set<Integer> incomingLectureIds = new HashSet<>();
        for (TutorCurriculumSaveRequest.LectureSaveItem lecReq : request.getLectures()) {
            if (lecReq.getLectureId() != null) {
                incomingLectureIds.add(lecReq.getLectureId());
            }
        }
        List<Integer> deletedLectureIds = new ArrayList<>();
        for (Lecture existingLec : existingLectures) {
            if (!deletedChapterIds.contains(existingLec.getChapterId())
                    && !incomingLectureIds.contains(existingLec.getLectureId())) {
                deletedLectureIds.add(existingLec.getLectureId());
            }
        }
        if (!deletedLectureIds.isEmpty()) {
            classImageFileRepository.deleteByLectureIdIn(deletedLectureIds);
            lectureRepository.deleteByClassIdAndLectureIdIn(classId, deletedLectureIds);
        }

        // [6단계] 챕터 수정 및 신규 챕터 등록 (chapterId가 null이면 신규 등록)
        Map<Integer, ClassChapter> existingChapterMap = new HashMap<>();
        for (ClassChapter existingCh : existingChapters) {
            existingChapterMap.put(existingCh.getChapterId(), existingCh);
        }

        int maxChapterId = classChapterRepository.findMaxChapterIdByClassId(classId);
        // 신규 챕터 소속 강의 매핑을 위한 (챕터 sortOrder -> 실제 chapterId) 맵
        Map<Integer, Integer> sortOrderToChapterIdMap = new HashMap<>();

        for (TutorCurriculumSaveRequest.ChapterSaveItem chReq : request.getChapters()) {
            if (chReq.getChapterId() != null) {
                ClassChapter targetChapter = existingChapterMap.get(chReq.getChapterId());
                if (targetChapter == null) {
                    throw new ResourceNotFoundException("존재하지 않는 챕터 번호입니다: " + chReq.getChapterId());
                }
                targetChapter.setTitle(chReq.getTitle().trim());
                targetChapter.setSortOrder(chReq.getSortOrder());
                classChapterRepository.save(targetChapter);

                sortOrderToChapterIdMap.put(chReq.getSortOrder(), targetChapter.getChapterId());
            } else {
                int newChapterId = ++maxChapterId;
                ClassChapter newChapter = new ClassChapter();
                newChapter.setClazz(clazz);
                newChapter.setChapterId(newChapterId);
                newChapter.setTitle(chReq.getTitle().trim());
                newChapter.setSortOrder(chReq.getSortOrder());
                classChapterRepository.save(newChapter);

                sortOrderToChapterIdMap.put(chReq.getSortOrder(), newChapterId);
            }
        }

        // [7단계] 강의 수정 및 신규 강의 등록
        Map<Integer, Lecture> existingLectureMap = new HashMap<>();
        for (Lecture existingLec : existingLectures) {
            existingLectureMap.put(existingLec.getLectureId(), existingLec);
        }

        for (TutorCurriculumSaveRequest.LectureSaveItem lecReq : request.getLectures()) {
            // 소속 챕터 번호 결정: chapterId가 있으면 사용, 없으면(신규 챕터 소속) chapterSortOrder로 매핑
            Integer resolvedChapterId = lecReq.getChapterId();
            if (resolvedChapterId == null && lecReq.getChapterSortOrder() != null) {
                resolvedChapterId = sortOrderToChapterIdMap.get(lecReq.getChapterSortOrder());
            }
            if (resolvedChapterId == null) {
                throw new InvalidStateException("강의가 소속된 챕터 정보가 올바르지 않습니다.");
            }

            if (lecReq.getLectureId() != null && existingLectureMap.containsKey(lecReq.getLectureId())) {
                // 기존 강의 수정 (기존 videoUrl, content, isPreview 유지)
                Lecture targetLecture = existingLectureMap.get(lecReq.getLectureId());
                targetLecture.setChapterId(resolvedChapterId);
                targetLecture.setTitle(lecReq.getTitle().trim());
                targetLecture.setDurationSec(lecReq.getDurationSec());
                targetLecture.setSortOrder(lecReq.getSortOrder());
                lectureRepository.save(targetLecture);
            } else {
                // 신규 강의 등록
                Lecture newLecture = new Lecture();
                newLecture.setClassId(classId);
                newLecture.setChapterId(resolvedChapterId);
                newLecture.setTitle(lecReq.getTitle().trim());
                newLecture.setDurationSec(lecReq.getDurationSec());
                newLecture.setSortOrder(lecReq.getSortOrder());
                newLecture.setIsPreview(false);
                lectureRepository.save(newLecture);
            }
        }
    }

    /**
     * 7. 클래스 수정 저장 (PATCH /tutor/{classId})
     */
    @Transactional
    public void updateClass(Integer classId, TutorClassUpdateRequest request, String loginEmail) {
        // [1단계] 강사 및 클래스 소유권 확인
        Tutor tutor = validateAndGetTutor(loginEmail);
        ClassEntity clazz = findAndValidateOwnedClass(classId, tutor);

        // [2단계] 상태별 수정 가능 여부 및 수정 금지 항목 검증
        ClassStatus status = clazz.getStatus();
        validateEditableClassStatus(status); // PENDING, STOPPED 즉시 차단

        // 할인율이 null이거나 0이면 둘 다 0(%)으로 통일하여 비교
        int currentDiscountRate = calculateDiscountRate(clazz.getPrice(), clazz.getSalePrice());
        int requestedDiscountRate = (request.getDiscountRate() == null) ? 0 : request.getDiscountRate();

        // PUBLISHED 또는 HIDDEN 상태에서 공통 수정 금지 항목: 진행 방식, 클래스 명, 카테고리
        if (status == ClassStatus.PUBLISHED || status == ClassStatus.HIDDEN) {
            if (request.getClassType() != null && request.getClassType() != clazz.getClassType()) {
                throw new InvalidStateException(status + " 상태에서는 진행 방식을 수정할 수 없습니다.");
            }
            if (request.getTitle() != null && !request.getTitle().trim().equals(clazz.getTitle())) {
                throw new InvalidStateException(status + " 상태에서는 클래스 명을 수정할 수 없습니다.");
            }
            if (request.getCategoryId() != null && !request.getCategoryId().equals(clazz.getCategory().getCategoryId())) {
                throw new InvalidStateException(status + " 상태에서는 카테고리를 수정할 수 없습니다.");
            }
        }

        // PUBLISHED 상태에서 추가 수정 금지 항목: 가격, 할인율
        if (status == ClassStatus.PUBLISHED) {
            if (request.getPrice() != null && !request.getPrice().equals(clazz.getPrice())) {
                throw new InvalidStateException("PUBLISHED 상태에서는 가격을 수정할 수 없습니다.");
            }
            if (requestedDiscountRate != currentDiscountRate) {
                throw new InvalidStateException("PUBLISHED 상태에서는 할인율을 수정할 수 없습니다.");
            }
        }

        // [3단계] 상태별 허용 항목 업데이트
        // 3-1. DRAFT 상태: 진행 방식, 클래스 명, 카테고리 변경 반영
        if (status == ClassStatus.DRAFT) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 카테고리입니다."));
            clazz.setClassType(request.getClassType());
            clazz.setTitle(request.getTitle().trim());
            clazz.setCategory(category);
        }

        // 3-2. DRAFT 또는 HIDDEN 상태: 가격, 할인율(10원 단위 절사 할인가) 변경 반영
        if (status == ClassStatus.DRAFT || status == ClassStatus.HIDDEN) {
            Integer newSalePrice = calculateSalePrice(request.getPrice(), request.getDiscountRate());
            clazz.setPrice(request.getPrice());
            clazz.setSalePrice(newSalePrice);
        }

        // 3-3. DRAFT, PUBLISHED, HIDDEN 공통 수정 가능 항목: 한 줄 소개, 상세 소개, 수업 장소, 배너 이미지
        clazz.setSummary(request.getSummary().trim());
        clazz.setDescription(request.getDescription());

        if (clazz.getClassType() == ClassType.OFFLINE) {
            if (request.getAddress() == null || request.getAddress().isBlank()) {
                throw new InvalidStateException("오프라인 클래스는 수업 장소가 필수입니다.");
            }
            String trimmedAddress = request.getAddress().trim();
            clazz.setAddress(trimmedAddress);
            clazz.setRegion(extractRegion(trimmedAddress));
        } else {
            clazz.setAddress(null);
            clazz.setRegion(null);
        }

        // 배너 이미지 파일이 새로 첨부된 경우에만 교체, 없으면 기존 이미지 유지
        MultipartFile bannerFile = request.getBannerImage();
        if (bannerFile != null && !bannerFile.isEmpty()) {
            validateBannerImage(bannerFile);
            List<String> storedPaths = fileStorageService.storeFiles(List.of(bannerFile));
            if (!storedPaths.isEmpty()) {
                clazz.setThumbnailUrl(storedPaths.get(0));
            }
        }

        // [4단계] DB 저장
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

    private ClassEntity findAndValidateOwnedClass(Integer classId, Tutor tutor) {
        ClassEntity clazz = classEntityRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 클래스입니다."));

        if (!clazz.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new AuthorizationFailureException("본인이 등록한 클래스만 조회하거나 수정할 수 있습니다.");
        }
        return clazz;
    }

    private void validateEditableClassStatus(ClassStatus status) {
        if (status == ClassStatus.PENDING) {
            throw new InvalidStateException("관리자 검토 대기(PENDING) 상태의 클래스는 수정할 수 없습니다.");
        }
        if (status == ClassStatus.STOPPED) {
            throw new AuthorizationFailureException("판매 중지(STOPPED) 상태의 클래스는 수정할 수 없습니다.");
        }
    }

    private void validateBannerImage(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.length() > MAX_FILE_NAME_LENGTH
                || name.contains("/") || name.contains("\\") || name.contains("..")
                || !name.contains(".")) {
            throw new InvalidStateException("배너 이미지 파일 이름이 올바르지 않습니다.");
        }
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new InvalidStateException("배너 이미지는 JPG, PNG 파일만 업로드할 수 있습니다.");
        }
        if (file.getSize() > MAX_BANNER_FILE_SIZE) {
            throw new InvalidStateException("배너 이미지는 5MB 이하만 업로드할 수 있습니다.");
        }
    }

    /**
     * 정가(price)와 할인율(0~100%)로 10원 단위 절사된 할인가(salePrice) 계산
     * - 할인율이 null이거나 0이면 할인 없음(null 반환)
     * - 할인율이 100이면 할인가 0원 반환
     */
    private Integer calculateSalePrice(Integer price, Integer discountRate) {
        if (discountRate == null || discountRate == 0) {
            return null;
        }
        if (discountRate < 0 || discountRate > 100) {
            throw new InvalidStateException("할인율은 0~100 사이의 숫자만 입력할 수 있습니다.");
        }
        return (price * (100 - discountRate) / 1000) * 10;
    }

    /**
     * 정가(price)와 할인가(salePrice)로 할인율(0~100%) 역산 (소수점 버림)
     * - 할인가가 없으면(null) 0 반환
     */
    private Integer calculateDiscountRate(Integer price, Integer salePrice) {
        if (salePrice == null || price == null || price <= 0 || salePrice >= price) {
            return 0;
        }
        return (price - salePrice) * 100 / price;
    }

    /**
     * 주소 문자열에서 앞 2어절(시/도 + 시/군/구) 추출 및 시/도 정규화
     */
    private String extractRegion(String address) {
        String[] tokens = address.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) {
            return null;
        }
        String sido = normalizeSido(tokens[0]);
        if ("세종".equals(sido) || tokens.length == 1) {
            return sido;
        }
        return sido + " " + tokens[1];
    }

    private String normalizeSido(String rawSido) {
        return switch (rawSido) {
            case "서울특별시", "서울시" -> "서울";
            case "부산광역시", "부산시" -> "부산";
            case "대구광역시", "대구시" -> "대구";
            case "인천광역시", "인천시" -> "인천";
            case "광주광역시", "광주시" -> "광주";
            case "대전광역시", "대전시" -> "대전";
            case "울산광역시", "울산시" -> "울산";
            case "세종특별자치시", "세종시" -> "세종";
            case "경기도" -> "경기";
            case "강원특별자치도", "강원도" -> "강원";
            case "충청북도" -> "충북";
            case "충청남도" -> "충남";
            case "전북특별자치도", "전라북도" -> "전북";
            case "전라남도" -> "전남";
            case "경상북도" -> "경북";
            case "경상남도" -> "경남";
            case "제주특별자치도", "제주도" -> "제주";
            default -> rawSido;
        };
    }

    private void validateCreateCurriculum(
            List<TutorClassCreateRequest.ChapterCreateItem> chapters,
            List<TutorClassCreateRequest.LectureCreateItem> lectures
    ) {
        if (chapters == null || chapters.isEmpty()) {
            throw new InvalidStateException("최소 1개 이상의 챕터를 등록해야 합니다.");
        }
        if (lectures == null || lectures.isEmpty()) {
            throw new InvalidStateException("커리큘럼에 강의를 1개 이상 추가해 주세요.");
        }
        Set<Integer> chapterSortOrders = new HashSet<>();
        for (TutorClassCreateRequest.ChapterCreateItem ch : chapters) {
            if (!chapterSortOrders.add(ch.getSortOrder())) {
                throw new InvalidStateException("챕터 정렬 순서가 중복되었습니다.");
            }
        }
        Set<Integer> chapterOrdersWithLectures = new HashSet<>();
        for (TutorClassCreateRequest.LectureCreateItem lec : lectures) {
            chapterOrdersWithLectures.add(lec.getChapterSortOrder());
        }
        if (!chapterOrdersWithLectures.containsAll(chapterSortOrders)) {
            throw new InvalidStateException("강의가 없는 빈 챕터가 존재합니다. 모든 챕터에 강의를 1개 이상 추가해 주세요.");
        }
    }

    private void validateSaveCurriculum(
            List<TutorCurriculumSaveRequest.ChapterSaveItem> chapters,
            List<TutorCurriculumSaveRequest.LectureSaveItem> lectures
    ) {
        if (chapters == null || chapters.isEmpty()) {
            throw new InvalidStateException("최소 1개 이상의 챕터를 등록해야 합니다.");
        }
        if (lectures == null || lectures.isEmpty()) {
            throw new InvalidStateException("커리큘럼에 강의를 1개 이상 추가해 주세요.");
        }

        // 모든 챕터에 최소 1개 이상의 강의가 존재하는지 검사
        for (TutorCurriculumSaveRequest.ChapterSaveItem ch : chapters) {
            boolean hasLecture = false;
            for (TutorCurriculumSaveRequest.LectureSaveItem lec : lectures) {
                boolean matchedById = (ch.getChapterId() != null && ch.getChapterId().equals(lec.getChapterId()));
                boolean matchedBySortOrder = (lec.getChapterSortOrder() != null && lec.getChapterSortOrder().equals(ch.getSortOrder()));
                if (matchedById || matchedBySortOrder) {
                    hasLecture = true;
                    break;
                }
            }
            if (!hasLecture) {
                throw new InvalidStateException("강의가 없는 빈 챕터가 존재합니다. 모든 챕터에 강의를 1개 이상 추가해 주세요.");
            }
        }
    }
}
