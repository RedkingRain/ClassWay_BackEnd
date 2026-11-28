package com.greenproject.classway.dto;

import com.greenproject.classway.global.ClassType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TutorClassCreateRequest {

    @NotNull(message = "진행 방식은 필수입니다.")
    private ClassType classType; // ONLINE / OFFLINE

    @NotBlank(message = "클래스 명은 필수입니다.")
    @Size(max = 100, message = "클래스 명은 100자 이하로 입력해주세요.")
    private String title;

    @NotNull(message = "카테고리는 필수입니다.")
    private Integer categoryId;

    @NotBlank(message = "클래스 한 줄 소개는 필수입니다.")
    @Size(max = 250, message = "클래스 한 줄 소개는 250자 이하로 입력해주세요.")
    private String summary;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 1000, message = "가격은 1,000원 이상이어야 합니다.")
    private Integer price;

    @Min(value = 0, message = "할인율은 0~100 사이의 숫자만 입력할 수 있습니다.")
    @Max(value = 100, message = "할인율은 0~100 사이의 숫자만 입력할 수 있습니다.")
    private Integer discountRate; // 선택 입력 (null 또는 0이면 할인 없음, 100이면 할인가 0원)

    @NotNull(message = "배너 이미지 파일은 필수입니다.")
    private MultipartFile bannerImage;

    @NotBlank(message = "클래스 상세 소개는 필수입니다.")
    private String description;

    @Size(max = 255, message = "수업 장소는 255자 이하로 입력해주세요.")
    private String address; // 오프라인인 경우 Service에서 필수 검증

    @Valid
    @NotEmpty(message = "커리큘럼 챕터는 최소 1개 이상이어야 합니다.")
    private List<ChapterCreateItem> chapters;

    @Valid
    @NotEmpty(message = "커리큘럼 강의는 최소 1개 이상이어야 합니다.")
    private List<LectureCreateItem> lectures;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterCreateItem {
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
    public static class LectureCreateItem {
        @NotNull(message = "소속된 챕터의 정렬 순서는 필수입니다.")
        private Integer chapterSortOrder;

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
