package com.greenproject.classway.service;

import com.greenproject.classway.dto.CategoryResponse;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 카테고리 (MAIN-002)
@Service
@AllArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    // 사용 중인 카테고리 목록 (번호 순서, 공개 클래스 수 포함)
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findActiveWithClassCount(ClassStatus.PUBLISHED);
    }
}