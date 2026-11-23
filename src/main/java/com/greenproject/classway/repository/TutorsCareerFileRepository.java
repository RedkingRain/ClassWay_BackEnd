package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Tutor;
import com.greenproject.classway.entity.TutorsCareerFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TutorsCareerFileRepository extends JpaRepository<TutorsCareerFile, Integer> {
    // 신청서(tutor)에 첨부된 제출 자료 파일 전부 조회, 올린 순서(file_id)대로 (관리자 강사 신청 상세용)
    List<TutorsCareerFile> findByTutorOrderByFileIdAsc(Tutor tutor);

    // 파일 번호 + 신청 번호가 둘 다 맞는 파일 1개 (다른 신청서의 파일 번호로는 못 찾음) - 제출 자료 보기용
    Optional<TutorsCareerFile> findByFileIdAndTutor_TutorId(Integer fileId, Integer tutorId);
}
