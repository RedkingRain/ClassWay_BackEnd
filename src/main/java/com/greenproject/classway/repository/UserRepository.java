package com.greenproject.classway.repository;

import com.greenproject.classway.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
