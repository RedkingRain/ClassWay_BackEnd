package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

// 문의 첨부 파일 보기 결과 (Service -> Controller 전달용, JSON 응답 아님)
@AllArgsConstructor
@Getter
public class InquiryFileDownload {
    private final Resource resource;
    private final String contentType;
    private final String fileName;
}