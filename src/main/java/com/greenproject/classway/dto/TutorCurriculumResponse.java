package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorCurriculumResponse {
    private List<ChapterDto> chapters;
    private List<LectureDto> lectures;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterDto {
        private Integer chapterId;  // 챕터 번호
        private String title;       // 챕터 명
        private Integer sortOrder;  // 정렬 순서
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LectureDto {
        private Integer lectureId;       // 강의 번호
        private Integer chapterId;       // 챕터 번호
        private String title;            // 강의 제목
        private Integer durationSec;     // 영상 길이 (초)
        private Integer sortOrder;       // 챕터 내 강의 정렬 순서
    }
}
