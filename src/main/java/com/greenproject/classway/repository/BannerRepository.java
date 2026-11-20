package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Integer> {
    // 순서 값이 같으면 번호 순으로 고정해서 새로고침할 때마다 순서가 바뀌지 않게 함
    @Query("""
            SELECT b FROM Banner b
            WHERE b.isActive = true
              AND b.startAt <= :now
              AND b.endAt >= :now
            ORDER BY b.sortOrder ASC, b.bannerId ASC
            """)
    List<Banner> findShowingBanners(@Param("now") LocalDateTime now);
}
