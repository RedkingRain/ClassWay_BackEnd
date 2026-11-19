package com.greenproject.classway.service;

import com.greenproject.classway.dto.TermsResponse;
import com.greenproject.classway.entity.Terms;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.repository.TermsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class TermsService {
    private final TermsRepository termsRepository;

    @Transactional(readOnly = true)
    public TermsResponse getLatestTerms(String termsType) {
        // 1. 전달받은 약관 타입 공백 제거 및 대문자 변환
        String normalizedType = termsType.trim().toUpperCase();

        // 2. 현재 시각 기준으로 효력이 발생한 가장 최신 약관 1건 조회
        Terms terms = termsRepository
                .findLatestEffectiveTerms(normalizedType, LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("해당하는 약관이 존재하지 않습니다."));

        // 3. 서비스에서 응답 DTO 생성 및 반환
        return new TermsResponse(
                terms.getId(),
                terms.getTermsType(),
                terms.getVersion(),
                terms.getTitle(),
                terms.getContent(),
                terms.getIsRequired(),
                terms.getEffectiveDate()
        );
    }
}
