package com.greenproject.classway.service;

import com.greenproject.classway.dto.ClassDetailResponse;
import com.greenproject.classway.entity.ClassChapter;
import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.entity.ClassSchedule;
import com.greenproject.classway.entity.Lecture;
import com.greenproject.classway.entity.Tutor;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import com.greenproject.classway.global.ReservationStatus;
import com.greenproject.classway.repository.ClassChapterRepository;
import com.greenproject.classway.repository.ClassEntityRepository;
import com.greenproject.classway.repository.ClassScheduleRepository;
import com.greenproject.classway.repository.LectureRepository;
import com.greenproject.classway.repository.ReservationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.greenproject.classway.dto.ReviewListResponse;
import com.greenproject.classway.entity.Reservation;
import com.greenproject.classway.entity.Review;
import com.greenproject.classway.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.greenproject.classway.dto.ScheduleResponse;
import com.greenproject.classway.global.ScheduleStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// 클래스 상세 정보 (CLASS-001 ~ 003)
@Service
@AllArgsConstructor
public class ClassService {
    private final ClassEntityRepository classEntityRepository;
    private final ClassChapterRepository classChapterRepository;
    private final LectureRepository lectureRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;

    // 수강평 한 번에 최대 몇 개까지 줄지 (size=100000 같은 요청 막기)
    private static final int MAX_REVIEW_PAGE_SIZE = 20;

    // loginEmail: 로그인한 사람의 이메일 (비로그인이면 null)
    @Transactional(readOnly = true)
    public ClassDetailResponse getClassDetail(Integer classId, String loginEmail) {

        // 1. 공개(PUBLISHED) 클래스만 보여줌
        //    작성 중 · 숨김 · 중지 클래스도 똑같이 「없음」으로 답해서, 있는지조차 알려주지 않음
        ClassEntity clazz = classEntityRepository.findByClassIdAndStatus(classId, ClassStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 강의입니다."));

        // 2. 커리큘럼: 챕터 목록 + 강의 전체를 한 번씩만 조회한 뒤, 챕터별로 나눠 담기
        List<ClassChapter> chapters = classChapterRepository.findByClazz_ClassIdOrderBySortOrderAsc(classId);
        List<Lecture> lectures = lectureRepository.findByChapter_Clazz_ClassIdOrderBySortOrderAsc(classId);

        List<ClassDetailResponse.ChapterInfo> chapterInfos = new ArrayList<>();
        for (ClassChapter chapter : chapters) {
            List<ClassDetailResponse.LectureInfo> lectureInfos = new ArrayList<>();
            for (Lecture lecture : lectures) {
                // 이 챕터에 속한 강의만 담기
                if (lecture.getChapter().getChapterId().equals(chapter.getChapterId())) {
                    lectureInfos.add(ClassDetailResponse.LectureInfo.builder()
                            .lectureId(lecture.getLectureId())
                            .title(lecture.getTitle())
                            .durationMinutes(toMinutes(lecture.getDurationSec()))
                            // 영상 주소(videoUrl)는 결제 전이라 담지 않음
                            .build());
                }
            }
            chapterInfos.add(ClassDetailResponse.ChapterInfo.builder()
                    .chapterId(chapter.getChapterId())
                    .title(chapter.getTitle())
                    .lectures(lectureInfos)
                    .build());
        }

        // 3. 총 강의 시간 (초 → 분)
        int totalSeconds = 0;
        for (Lecture lecture : lectures) {
            if (lecture.getDurationSec() != null) {
                totalSeconds += lecture.getDurationSec();
            }
        }

        // 4. 할인율 % (소수점 버림) — 할인가가 없거나 정가보다 크면 0
        Integer price = clazz.getPrice();
        Integer salePrice = clazz.getSalePrice();
        int discountRate = 0;
        if (salePrice != null && salePrice < price) {
            discountRate = (price - salePrice) * 100 / price; // 정수끼리 나누면 소수점이 자동으로 버려짐
        }

        // 5. 수강생 수: 예약 확정 , 수업 완료 건수 (취소 제외)
        long studentCount = reservationRepository.countByClazz_ClassIdAndStatusIn(
                classId, List.of(ReservationStatus.RESERVED, ReservationStatus.COMPLETED));

        // 6. 오프라인 최대 정원 (온라인은 null)
        boolean isOnline = clazz.getClassType() == ClassType.ONLINE;
        Integer maxCapacity = null;
        if (!isOnline) {
            maxCapacity = classScheduleRepository.findTopByClazz_ClassIdOrderByCapacityDesc(classId)
                    .map(ClassSchedule::getCapacity)
                    .orElse(null);
        }

        // 7. 이미 수강 중인지 — 로그인했고 온라인 클래스일 때만 확인
        //    오프라인은 다른 날짜로 다시 예약할 수 있어서 항상 false
        boolean isEnrolled = false;
        if (loginEmail != null && isOnline) {
            isEnrolled = reservationRepository.existsByUser_EmailAndClazz_ClassIdAndStatus(
                    loginEmail, classId, ReservationStatus.RESERVED);
        }

        // 8. 강사 — 닉네임만 (실명 · 이메일 · 연락처는 담지 않음)
        Tutor tutor = clazz.getTutor();
        User tutorUser = tutor.getUser();
        long tutorClassCount = classEntityRepository.countByTutor_TutorIdAndStatus(
                tutor.getTutorId(), ClassStatus.PUBLISHED);

        ClassDetailResponse.TutorInfo tutorInfo = ClassDetailResponse.TutorInfo.builder()
                .tutorId(tutor.getTutorId())
                .nickname(tutorUser.getNickname())
                .profileImage(tutorUser.getProfileImage())
                .intro(tutor.getIntro())
                .career(tutor.getCareer())
                .classCount(tutorClassCount)
                .build();

        // 9. 전부 담아서 돌려주기
        return ClassDetailResponse.builder()
                .classId(clazz.getClassId())
                .title(clazz.getTitle())
                .summary(clazz.getSummary())
                .description(clazz.getDescription())
                .classType(clazz.getClassType().name())
                .category(ClassDetailResponse.CategoryInfo.builder()
                        .categoryId(clazz.getCategory().getCategoryId())
                        .name(clazz.getCategory().getName())
                        .build())
                .thumbnailUrl(clazz.getThumbnailUrl())
                .price(price)
                .salePrice(salePrice)
                .discountRate(discountRate)
                .avgRating(clazz.getAvgRating())
                .reviewCount(clazz.getReviewCount())
                .wishCount(clazz.getWishCount())
                .studentCount(studentCount)
                .totalDurationMinutes(toMinutes(totalSeconds))
                .lectureCount(lectures.size())
                .region(clazz.getRegion())
                .address(clazz.getAddress())
                .maxCapacity(maxCapacity)
                .isEnrolled(isEnrolled)
                .tutor(tutorInfo)
                .chapters(chapterInfos)
                .build();
    }

    // 수강평 목록 (CLASS-001 수강평 탭)
    // sort: latest(최신순) / rating(별점순), page: 0부터 시작, size: 한 번에 몇 개
    @Transactional(readOnly = true)
    public ReviewListResponse getReviews(Integer classId, String sort, int page, int size) {

        // 1. 공개 클래스인지 확인 (상세와 같은 규칙)
        if (classEntityRepository.findByClassIdAndStatus(classId, ClassStatus.PUBLISHED).isEmpty()) {
            throw new ResourceNotFoundException("존재하지 않는 강의입니다.");
        }

        // 2. 정렬 — 정해둔 두 가지만 허용, 그 외 값은 최신순
        //    요청 값을 그대로 정렬 칸 이름으로 쓰면 아무 칸으로나 정렬할 수 있어서 위험
        Sort order;
        if ("rating".equals(sort)) {
            order = Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt")); // 별점 같으면 최신순
        } else {
            order = Sort.by(Sort.Order.desc("createdAt"));
        }

        // 3. 페이지 번호 · 개수 범위 제한 (음수 · 너무 큰 값 막기)
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_REVIEW_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, order);

        // 4. 조회 (숨김 · 삭제 제외)
        Page<Review> reviewPage = reviewRepository
                .findByReservation_Clazz_ClassIdAndIsHiddenFalseAndIsDeletedFalse(classId, pageable);

        // 5. 화면용으로 옮겨 담기 — 작성자는 닉네임만
        List<ReviewListResponse.ReviewItem> items = new ArrayList<>();
        for (Review review : reviewPage.getContent()) {
            Reservation reservation = review.getReservation();
            ClassSchedule schedule = reservation.getSchedule(); // 온라인은 null

            items.add(ReviewListResponse.ReviewItem.builder()
                    .reviewId(review.getReservationId())
                    .writerNickname(reservation.getUser().getNickname())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .attendedAt(schedule != null ? schedule.getStartAt() : null)
                    .createdAt(review.getCreatedAt())
                    .build());
        }

        return ReviewListResponse.builder()
                .reviews(items)
                .totalCount(reviewPage.getTotalElements()) // 숨김 · 삭제 뺀 전체 개수
                .hasNext(reviewPage.hasNext())             // 다음 페이지 있는지
                .build();
    }

    // 일정 목록 (CLASS-002 날짜 · 시간 선택)
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedules(Integer classId) {

        // 1. 공개 클래스인지 확인
        ClassEntity clazz = classEntityRepository.findByClassIdAndStatus(classId, ClassStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 강의입니다."));

        // 2. 온라인은 일정이 없음 → 빈 목록
        if (clazz.getClassType() == ClassType.ONLINE) {
            return List.of();
        }

        // 3. 지금 이후 시작하는 예약 가능 · 마감 일정 (취소 · 종료 제외)
        List<ClassSchedule> schedules = classScheduleRepository.findUpcomingSchedules(
                classId, LocalDateTime.now(), List.of(ScheduleStatus.OPEN, ScheduleStatus.FULL));

        // 4. 화면용으로 옮겨 담기
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        List<ScheduleResponse> result = new ArrayList<>();
        for (ClassSchedule schedule : schedules) {
            // 남은 자리 (예약 인원이 정원보다 많게 잘못 들어가 있어도 0 아래로는 안 내려가게)
            int remainingSeats = Math.max(0, schedule.getCapacity() - schedule.getReservedCount());

            result.add(ScheduleResponse.builder()
                    .scheduleId(schedule.getScheduleId())
                    .startAt(schedule.getStartAt())
                    .runningTime(schedule.getRunningTime() != null
                            ? schedule.getRunningTime().format(timeFormat)
                            : null)
                    .remainingSeats(remainingSeats)
                    .status(schedule.getStatus().name())
                    .build());
        }
        return result;
    }

    // 초 → 분 (반올림, 값이 없으면 0)
    private Integer toMinutes(Integer seconds) {
        if (seconds == null) return 0;
        return Math.round(seconds / 60.0f);
    }
}