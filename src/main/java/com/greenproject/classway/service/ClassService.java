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

    // 초 → 분 (반올림, 값이 없으면 0)
    private Integer toMinutes(Integer seconds) {
        if (seconds == null) return 0;
        return Math.round(seconds / 60.0f);
    }
}