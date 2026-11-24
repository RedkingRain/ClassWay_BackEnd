package com.greenproject.classway.service;

import com.greenproject.classway.dto.ClassQnaCreateRequest;
import com.greenproject.classway.dto.ClassQnaListResponse;
import com.greenproject.classway.entity.ClassEntity;
import com.greenproject.classway.entity.ClassQna;
import com.greenproject.classway.entity.User;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.exceptions.UnauthenticatedException;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.UserRole;
import com.greenproject.classway.repository.ClassEntityRepository;
import com.greenproject.classway.repository.ClassQnaRepository;
import com.greenproject.classway.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 클래스 상세 Q&A 탭 (CLASS-003)
@Service
@AllArgsConstructor
public class ClassQnaService {
    private final ClassQnaRepository classQnaRepository;
    private final ClassEntityRepository classEntityRepository;
    private final UserRepository userRepository;

    // size=100000 같은 요청 막기 (수강평 목록과 같은 한도)
    private static final int MAX_QNA_PAGE_SIZE = 20;

    // loginEmail: 로그인한 사람의 이메일 (비로그인이면 null)
    @Transactional(readOnly = true)
    public ClassQnaListResponse getQnas(Integer classId, String loginEmail, int page, int size) {
        ClassEntity clazz = findPublishedClass(classId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_QNA_PAGE_SIZE);
        Page<ClassQna> qnaPage = classQnaRepository.findQnaList(classId, PageRequest.of(safePage, safeSize));

        // 강사, 관리자 여부는 문의마다 같으니 한 번만 계산
        Integer loginUserId = null;
        boolean isTutorOrAdmin = false;
        if (loginEmail != null) {
            User loginUser = userRepository.findByEmail(loginEmail).orElse(null);
            if (loginUser != null) {
                loginUserId = loginUser.getId();
                boolean isClassTutor = clazz.getTutor().getUser().getId().equals(loginUserId);
                boolean isAdmin = loginUser.getRole() == UserRole.ADMIN;
                isTutorOrAdmin = isClassTutor || isAdmin;
            }
        }

        List<ClassQnaListResponse.QnaItem> items = new ArrayList<>();
        for (ClassQna qna : qnaPage.getContent()) {
            boolean isWriter = qna.getUser().getId().equals(loginUserId);
            boolean canView = !qna.getIsSecret() || isWriter || isTutorOrAdmin;

            // 권한이 없으면 내용을 아예 내려보내지 않음 (프론트에서 가리는 것만으로는 개발자 도구로 보임)
            items.add(ClassQnaListResponse.QnaItem.builder()
                    .qnaId(qna.getQnaId())
                    .writerNickname(qna.getUser().getNickname())
                    .isSecret(qna.getIsSecret())
                    .isAnswered(qna.getAnswer() != null)
                    .canView(canView)
                    .question(canView ? qna.getQuestion() : null)
                    .answer(canView ? qna.getAnswer() : null)
                    .answeredAt(canView ? qna.getAnsweredAt() : null)
                    .createdAt(qna.getCreatedAt())
                    .build());
        }

        return ClassQnaListResponse.builder()
                .qnas(items)
                .totalCount(qnaPage.getTotalElements())
                .hasNext(qnaPage.hasNext())
                .build();
    }

    @Transactional
    public Integer createQna(Integer classId, String loginEmail, ClassQnaCreateRequest request) {
        User user = userRepository.findByEmail(loginEmail)
                .orElseThrow(() -> new UnauthenticatedException("로그인이 필요합니다."));
        ClassEntity clazz = findPublishedClass(classId);

        ClassQna qna = new ClassQna();
        qna.setClazz(clazz);
        qna.setUser(user);
        qna.setQuestion(request.getQuestion().trim());
        qna.setIsSecret(Boolean.TRUE.equals(request.getIsSecret()));
        classQnaRepository.save(qna);

        return qna.getQnaId();
    }

    // 작성 중, 숨김 클래스도 같은 404로 답해서 있는지조차 알리지 않음 (상세 API와 같은 규칙)
    private ClassEntity findPublishedClass(Integer classId) {
        return classEntityRepository.findByClassIdAndStatus(classId, ClassStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 강의입니다."));
    }
}