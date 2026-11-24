package com.greenproject.classway.service;

import com.greenproject.classway.dto.StudyDashboardResponse;
import com.greenproject.classway.entity.ClassChapter;
import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.entity.Lecture;
import com.greenproject.classway.entity.StudyPost;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.AuthorizationFailureException;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.exceptions.UnauthenticatedException;
import com.greenproject.classway.global.ClassType;
import com.greenproject.classway.global.ReservationStatus;
import com.greenproject.classway.global.StudyPostType;
import com.greenproject.classway.global.UserRole;
import com.greenproject.classway.repository.ClassChapterRepository;
import com.greenproject.classway.repository.ClassEntityRepository;
import com.greenproject.classway.repository.LectureRepository;
import com.greenproject.classway.repository.ReservationRepository;
import com.greenproject.classway.repository.StudyPostRepository;
import com.greenproject.classway.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 클래스 학습 페이지 (STUDY-001 ~ 004)
@Service
@AllArgsConstructor
public class StudyService {
    private final UserRepository userRepository;
    private final ClassEntityRepository classEntityRepository;
    private final ReservationRepository reservationRepository;
    private final ClassChapterRepository classChapterRepository;
    private final LectureRepository lectureRepository;
    private final StudyPostRepository studyPostRepository;

    // 수업이 끝난(COMPLETED) 수강생도 공지와 자료는 계속 볼 수 있게 함
    private static final List<ReservationStatus> STUDY_STATUSES =
            List.of(ReservationStatus.RESERVED, ReservationStatus.COMPLETED);


    @Transactional(readOnly = true)
    public StudyDashboardResponse getDashboard(Integer classId) {
        String loginEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User loginUser = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new UnauthenticatedException("로그인이 필요합니다."));

        // 공개 상태는 따지지 않음: 비공개로 바뀐 뒤에도 산 사람은 들어와야 함
        ClassEntity clazz = classEntityRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 강의입니다."));

        checkStudyAccess(clazz, loginUser);
        boolean isTutor = isClassTutor(clazz, loginUser);

        List<StudyDashboardResponse.ChapterInfo> chapterInfos = new ArrayList<>();
        LocalDateTime nextScheduleAt = null;
        if (clazz.getClassType() == ClassType.ONLINE) {
            chapterInfos = getChapterInfos(classId);
        } else if (!isTutor) {
            nextScheduleAt = findNextScheduleAt(classId, loginUser.getEmail());
        }

        return StudyDashboardResponse.builder()
                .classId(clazz.getClassId())
                .title(clazz.getTitle())
                .classType(clazz.getClassType().name())
                .thumbnailUrl(clazz.getThumbnailUrl())
                .tutorNickname(clazz.getTutor().getUser().getNickname())
                .address(clazz.getAddress())
                .nextScheduleAt(nextScheduleAt)
                .isTutor(isTutor)
                .chapters(chapterInfos)
                .posts(getPostInfos(classId))
                .build();
    }

    // 클래스 강사, 관리자, 수강생만 통과
    private void checkStudyAccess(ClassEntity clazz, User loginUser) {
        if (isClassTutor(clazz, loginUser)) {
            return;
        }
        if (loginUser.getRole() == UserRole.ADMIN) {
            return;
        }
        long reservationCount = reservationRepository.countStudyReservation(
                loginUser.getEmail(), clazz.getClassId(), STUDY_STATUSES);
        if (reservationCount == 0) {
            throw new AuthorizationFailureException("수강 중인 회원만 볼 수 있습니다.");
        }
    }

    private boolean isClassTutor(ClassEntity clazz, User loginUser) {
        return clazz.getTutor().getUser().getId().equals(loginUser.getId());
    }

    private List<StudyDashboardResponse.ChapterInfo> getChapterInfos(Integer classId) {
        // 챕터, 강의를 한 번씩만 조회한 뒤 챕터별로 나눠 담기 (챕터마다 조회하면 N+1)
        List<ClassChapter> chapters = classChapterRepository.findByClazz_ClassIdOrderBySortOrderAsc(classId);
        List<Lecture> lectures = lectureRepository.findByClassIdOrderBySortOrderAsc(classId);

        List<StudyDashboardResponse.ChapterInfo> chapterInfos = new ArrayList<>();
        for (ClassChapter chapter : chapters) {
            List<StudyDashboardResponse.LectureInfo> lectureInfos = new ArrayList<>();
            for (Lecture lecture : lectures) {
                if (lecture.getChapter().getChapterId().equals(chapter.getChapterId())) {
                    lectureInfos.add(StudyDashboardResponse.LectureInfo.builder()
                            .lectureId(lecture.getLectureId())
                            .title(lecture.getTitle())
                            .durationMinutes(toMinutes(lecture.getDurationSec()))
                            .build());
                }
            }
            chapterInfos.add(StudyDashboardResponse.ChapterInfo.builder()
                    .chapterId(chapter.getChapterId())
                    .title(chapter.getTitle())
                    .lectures(lectureInfos)
                    .build());
        }
        return chapterInfos;
    }

    private LocalDateTime findNextScheduleAt(Integer classId, String email) {
        List<LocalDateTime> startTimes = reservationRepository.findNextScheduleStartAt(
                email, classId, STUDY_STATUSES, LocalDateTime.now(), PageRequest.of(0, 1));
        if (startTimes.isEmpty()) {
            return null;
        }
        return startTimes.get(0);
    }

    private List<StudyDashboardResponse.PostInfo> getPostInfos(Integer classId) {
        List<StudyDashboardResponse.PostInfo> postInfos = new ArrayList<>();
        for (StudyPost post : studyPostRepository.findBoardList(classId, StudyPostType.NOTICE)) {
            postInfos.add(StudyDashboardResponse.PostInfo.builder()
                    .postId(post.getPostId())
                    .type(post.getType().name())
                    .title(post.getTitle())
                    .createdAt(post.getCreatedAt())
                    .viewCount(post.getViewCount())
                    .build());
        }
        return postInfos;
    }

    private Integer toMinutes(Integer seconds) {
        if (seconds == null) {
            return 0;
        }
        return Math.round(seconds / 60.0f);
    }
}