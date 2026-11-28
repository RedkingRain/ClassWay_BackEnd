package com.greenproject.classway.repository;

import com.greenproject.classway.entity.ClassImageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassImageFileRepository extends JpaRepository<ClassImageFile, Integer> {
    @Modifying
    @Query("DELETE FROM ClassImageFile f WHERE f.lecture.lectureId IN :lectureIds")
    void deleteByLectureIdIn(@Param("lectureIds") List<Integer> lectureIds);
}
