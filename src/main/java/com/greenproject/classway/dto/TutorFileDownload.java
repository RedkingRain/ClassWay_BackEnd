package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

/**
 * 강사 제출 자료 보기 결과 (Service -> Controller 전달용, JSON 응답 아님) - ADMIN-002
 * - Controller가 이 값으로 파일 바이너리 응답(Content-Type, Content-Disposition)을 만든다
 */
@AllArgsConstructor
@Getter
public class TutorFileDownload {
    private final Resource resource;     // 디스크의 실제 파일
    private final String contentType;    // 예: application/pdf (모르면 application/octet-stream)
    private final String fileName;       // 화면에 보일 원래 파일명 (uuid 뗀 이름)
}
