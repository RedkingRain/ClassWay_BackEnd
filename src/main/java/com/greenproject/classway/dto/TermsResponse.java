package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsResponse {
    private Integer id;
    private String termsType;
    private String version;
    private String title;
    private String content;
    private Boolean isRequired;
    private LocalDateTime effectiveDate;
}
