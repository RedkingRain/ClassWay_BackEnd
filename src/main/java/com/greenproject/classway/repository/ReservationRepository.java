package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Reservation;
import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ReservationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    // 수강생 수: 예약 확정(RESERVED) , 수업 완료(COMPLETED) 건수
    long countByClazz_ClassIdAndStatusIn(Integer classId, List<ReservationStatus> statuses);
    // 이 회원이 이 클래스를 수강 중인지 (로그인 토큰의 이메일로 확인)
    boolean existsByUser_EmailAndClazz_ClassIdAndStatus(String email, Integer classId, ReservationStatus status);

    // 학습 페이지 입장 권한: 들어올 수 있는 상태의 예약 수 (0이면 권한 없음)
    @Query("""
            SELECT COUNT(r)
            FROM Reservation r
            WHERE r.user.id = :userId
              AND r.clazz.classId = :classId
              AND r.status IN :statuses
            """)
    long countStudyReservation(@Param("userId") Integer userId,
                               @Param("classId") Integer classId,
                               @Param("statuses") List<ReservationStatus> statuses);

    // 오프라인 상단 일시: 내 예약 회차 중 앞으로 남은 가장 가까운 것 (pageable로 1개만)
    @Query("""
            SELECT r.schedule.startAt
            FROM Reservation r
            WHERE r.user.id = :userId
              AND r.clazz.classId = :classId
              AND r.status IN :statuses
              AND r.schedule.startAt >= :now
            ORDER BY r.schedule.startAt ASC
            """)
    List<LocalDateTime> findNextScheduleStartAt(@Param("userId") Integer userId,
                                                @Param("classId") Integer classId,
                                                @Param("statuses") List<ReservationStatus> statuses,
                                                @Param("now") LocalDateTime now,
                                                Pageable pageable);

    // 최근 예약 많은 클래스 번호 (취소 제외, 공개 클래스만, 예약 수 같으면 최신 등록순)
    // 몇 개까지 꺼낼지는 pageable로 정함 (TOP 10 → PageRequest.of(0, 10))
    @Query("""
            SELECT r.clazz.classId
            FROM Reservation r
            WHERE r.reservedAt >= :since
              AND r.status <> :canceled
              AND r.clazz.status = :published
            GROUP BY r.clazz.classId
            ORDER BY COUNT(r) DESC, r.clazz.classId DESC
            """)
    List<Integer> findPopularClassIds(@Param("since") LocalDateTime since,
                                      @Param("canceled") ReservationStatus canceled,
                                      @Param("published") ClassStatus published,
                                      Pageable pageable);

    // 온라인 수강생 수 집계용 프로젝션 인터페이스
    interface ReservationCountProjection {
        Integer getClassId();
        Long getStudentCount();
    }

    // 여러 클래스의 유효 수강생 수를 한 번에 조회
    @Query("""
            SELECT r.clazz.classId AS classId, COUNT(r) AS studentCount
            FROM Reservation r
            WHERE r.clazz.classId IN :classIds
              AND r.status IN :statuses
            GROUP BY r.clazz.classId
            """)
    List<ReservationCountProjection> countReservationsByClassIds(
            @Param("classIds") List<Integer> classIds,
            @Param("statuses") List<ReservationStatus> statuses
    );
}
