package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Integer> {
    Optional<Tutor> findByUserId(Integer userId);
}
