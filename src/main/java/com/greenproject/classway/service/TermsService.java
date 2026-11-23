package com.greenproject.classway.service;

import com.greenproject.classway.dto.TermsResponse;
import com.greenproject.classway.dto.TermsVersionResponse;
import com.greenproject.classway.entity.Terms;
import com.greenproject.classway.exceptions.ResourceNotFoundException;
import com.greenproject.classway.repository.TermsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class TermsService {
    private final TermsRepository termsRepository;

    @Transactional(readOnly = true)
    public TermsResponse getLatestTerms(String termsType) {
        // 1. 전달받은 약관 타입 공백 제거 및 대문자 변환
        String normalizedType = termsType.trim().toUpperCase(Locale.ROOT);

        // 2. 현재 시각 기준으로 효력이 발생한 가장 최신 약관 1건 조회
        Terms terms = termsRepository
                .findLatestEffectiveTerms(normalizedType, LocalDateTime.now())
                .orElseThrow(() -> new ResourceNotFoundException("해당하는 약관이 존재하지 않습니다."));

        // 3. 서비스에서 응답 DTO 생성 및 반환
        return toTermsResponse(terms);
    }

    @Transactional(readOnly = true)
    public List<TermsVersionResponse> getTermsVersions(String termsType) {
        String normalizedType = termsType.trim().toUpperCase(Locale.ROOT);
        List<Terms> termsList = termsRepository.findEffectiveVersions(normalizedType, LocalDateTime.now());

        List<TermsVersionResponse> result = new ArrayList<>();
        for (Terms terms : termsList) {
            result.add(new TermsVersionResponse(terms.getId(), terms.getVersion(), terms.getEffectiveDate()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public TermsResponse getTermsVersion(String termsType, Integer id) {
        String normalizedType = termsType.trim().toUpperCase(Locale.ROOT);
        Terms terms = termsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("해당하는 약관이 존재하지 않습니다."));

        // 다른 유형의 약관이나 아직 시행 전인 약관이 주소만 바꿔서 보이지 않게 막음
        if (!terms.getTermsType().equals(normalizedType) || terms.getEffectiveDate().isAfter(LocalDateTime.now())) {
            throw new ResourceNotFoundException("해당하는 약관이 존재하지 않습니다.");
        }
        return toTermsResponse(terms);
    }

    private TermsResponse toTermsResponse(Terms terms) {
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
