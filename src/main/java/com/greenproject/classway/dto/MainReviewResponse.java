package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 작성자는 닉네임만 담음 (실명, 이메일은 보내지 않음)
@Getter
@Builder
@AllArgsConstructor
public class MainReviewResponse {
    private final Integer reviewId;
    private final Byte rating;
    private final String content;
    private final String writerNickname;
    private final Integer classId;
    private final String classTitle;
    private final String classType;
    private final String thumbnailUrl;
}