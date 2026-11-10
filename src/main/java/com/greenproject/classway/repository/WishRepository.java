package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import com.greenproject.classway.entity.WishId;

public interface WishRepository extends JpaRepository<Wish, WishId> {
}
