package com.greenproject.classway.repository;

import com.greenproject.classway.entity.AccessTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessTokenBlacklistRepository extends JpaRepository<AccessTokenBlacklist, Integer> {
}
