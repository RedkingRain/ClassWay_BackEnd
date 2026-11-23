package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 클래스 Q&A 목록 응답 (GET /classes/{classId}/qnas) - CLASS-003 Q&A 탭
@Getter
@Builder
@AllArgsConstructor
public class ClassQnaListResponse {
    private final List<QnaItem> qnas;  // 이번 페이지 문의
    private final Long totalCount;     // 삭제 안 된 문의 전체 개수 -> [문의 N개]
    private final Boolean hasNext;     // 다음 페이지가 있는지 -> 더보기 버튼

    // 문의 한 줄 - 작성자는 닉네임만, 볼 권한이 없으면 질문, 답변은 null
    @Getter
    @Builder
    @AllArgsConstructor
    public static class QnaItem {
        private final Integer qnaId;
        private final String writerNickname;
        private final Boolean isSecret;
        private final Boolean isAnswered;      // 비밀글도 답변 완료, 대기는 보여 줌
        private final Boolean canView;
        private final String question;
        private final String answer;
        private final LocalDateTime answeredAt;
        private final LocalDateTime createdAt;
    }
}