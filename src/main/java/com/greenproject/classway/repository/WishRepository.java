package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Wish;
import com.greenproject.classway.entity.WishId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishRepository extends JpaRepository<Wish, WishId> {
    // 로그인한 사람이 이 클래스를 찜했는지 (상세 화면 하트 색)
    @Query("""
            SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END
            FROM Wish w
            WHERE w.user.email = :email
              AND w.clazz.classId = :classId
            """)
    boolean existsWish(@Param("email") String email, @Param("classId") Integer classId);
}