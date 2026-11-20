package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BannerResponse {
    private final Integer bannerId;
    private final String title;
    private final String imageUrl;
    private final String linkUrl;
}
