package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Integer> {
}
