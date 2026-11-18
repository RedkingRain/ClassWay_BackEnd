package com.greenproject.classway.repository;

import com.greenproject.classway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(int userId);
    List<User> findByNameAndPhone(String name, String phone);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
