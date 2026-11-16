package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Reservation;
import com.greenproject.classway.global.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    // 수강생 수: 예약 확정(RESERVED) , 수업 완료(COMPLETED) 건수
    long countByClazz_ClassIdAndStatusIn(Integer classId, List<ReservationStatus> statuses);
    // 이 회원이 이 클래스를 수강 중인지 (로그인 토큰의 이메일로 확인)
    boolean existsByUser_EmailAndClazz_ClassIdAndStatus(String email, Integer classId, ReservationStatus status);
}
