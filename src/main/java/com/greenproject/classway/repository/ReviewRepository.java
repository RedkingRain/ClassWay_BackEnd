package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    // 클래스의 보이는 수강평만 페이지 단위로 (관리자 숨김, 작성자 삭제 제외)
    // 수강평 → 예약 → 클래스 번호로 찾기
    // @EntityGraph: 예약, 작성자, 수업 일정을 같이 가져와서 N+1 방지
    @EntityGraph(attributePaths = {"reservation", "reservation.user", "reservation.schedule"})
    Page<Review> findByReservation_Clazz_ClassIdAndIsHiddenFalseAndIsDeletedFalse(Integer classId, Pageable pageable);
}