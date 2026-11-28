package com.greenproject.classway.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TutorCurriculumSaveRequest {

    @Valid
    @NotEmpty(message = "챕터는 최소 1개 이상이어야 합니다.")
    private List<ChapterSaveItem> chapters;

    @Valid
    @NotEmpty(message = "강의는 최소 1개 이상이어야 합니다.")
    private List<LectureSaveItem> lectures;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterSaveItem {
        private Integer chapterId; // 값이 없으면(null) 신규 등록 항목

        @NotBlank(message = "챕터 명은 필수입니다.")
        @Size(max = 100, message = "챕터 명은 100자 이하로 입력해주세요.")
        private String title;

        @NotNull(message = "챕터 정렬 순서는 필수입니다.")
        private Integer sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LectureSaveItem {
        private Integer lectureId;        // 값이 없으면(null) 신규 강의
        private Integer chapterId;        // 기존 챕터 소속이면 해당 챕터 번호, 신규 챕터 소속이면 null
        private Integer chapterSortOrder; // 신규 챕터와 매핑할 때 사용하는 소속 챕터의 정렬 순서

        @NotBlank(message = "강의 제목은 필수입니다.")
        @Size(max = 100, message = "강의 제목은 100자 이하로 입력해주세요.")
        private String title;

        @NotNull(message = "영상 길이는 필수입니다.")
        @Min(value = 1, message = "영상 길이는 1초 이상이어야 합니다.")
        private Integer durationSec;

        @NotNull(message = "챕터 내 강의 정렬 순서는 필수입니다.")
        private Integer sortOrder;
    }
}
