package com.greenproject.classway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 강사 신청 상세의 제출 자료 파일 1개 (ADMIN-002)
 * - 서버 저장 경로(file_url)는 내보내지 않음 (서버 폴더 노출 방지, 파일 보기 API는 추후)
 */
@AllArgsConstructor
@Getter
public class TutorFileResponse {
    private final Integer fileId;    // 파일 번호 (나중에 파일 보기/다운로드 API에서 사용)
    private final String fileName;   // 원래 파일명 (서버가 저장 이름 앞의 uuid를 떼고 보냄)
}
