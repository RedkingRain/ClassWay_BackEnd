package com.greenproject.classway.controller;

import com.greenproject.classway.dto.ApiResponse;
import com.greenproject.classway.dto.InquiryCreateRequest;
import com.greenproject.classway.dto.InquiryFileDownload;
import com.greenproject.classway.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Tag(name = "ClassWay 1:1 문의 API", description = "고객센터 1:1 문의 API 모듈입니다.")
@RestController
@RequestMapping("/inquiries")
@AllArgsConstructor
public class InquiryController {
    private final InquiryService inquiryService;

    @Operation(summary = "1:1 문의 등록 API", description = "로그인 회원의 1:1 문의 등록 (첨부 파일 JPG, PNG, PDF 최대 3개)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> createInquiry(@Valid @ModelAttribute InquiryCreateRequest request) {
        inquiryService.createInquiry(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "1:1 문의 첨부 파일 보기 API",
            description = "파일 바이너리 응답 (JSON 아님). 작성자 본인과 관리자만 가능, 그 외에는 404")
    @GetMapping("/{inquiryId}/files/{fileId}")
    public ResponseEntity<Resource> getInquiryFile(
            @PathVariable Integer inquiryId,
            @PathVariable Integer fileId
    ) {
        InquiryFileDownload file = inquiryService.getInquiryFile(inquiryId, fileId);

        // 브라우저에서 바로 열리고(inline) 한글 파일 이름이 깨지지 않게(UTF-8) 함
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.getResource());
    }
}