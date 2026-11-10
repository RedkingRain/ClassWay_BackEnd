package com.greenproject.classway.repository;

import com.greenproject.classway.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Integer> {
}
