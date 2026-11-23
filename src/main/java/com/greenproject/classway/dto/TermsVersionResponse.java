package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TermsVersionResponse {
    private final Integer id;
    private final String version;
    private final LocalDateTime effectiveDate;
}