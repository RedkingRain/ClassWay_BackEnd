package com.greenproject.classway.repository;

import com.greenproject.classway.dto.CategoryResponse;
import com.greenproject.classway.entity.Category;
import com.greenproject.classway.global.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // 사용 중인 카테고리 + 공개 클래스 수 (MAIN-002), 번호 순서
    // LEFT JOIN: 클래스가 0개인 카테고리도 나옴 (classCount = 0)
    @Query("""
            SELECT new com.greenproject.classway.dto.CategoryResponse(cat.categoryId, cat.name, cat.iconUrl, COUNT(c))
            FROM Category cat
            LEFT JOIN ClassEntity c ON c.category = cat AND c.status = :status
            WHERE cat.isActive = true
            GROUP BY cat.categoryId, cat.name, cat.iconUrl
            ORDER BY cat.categoryId
            """)
    List<CategoryResponse> findActiveWithClassCount(@Param("status") ClassStatus status);
}