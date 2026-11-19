package com.greenproject.classway.repository;

import com.greenproject.classway.entity.Terms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TermsRepository extends JpaRepository<Terms, Integer> {

    @Query(value = """
        SELECT *
        FROM terms
        WHERE terms_type = :termsType
          AND effective_date <= :now
        ORDER BY effective_date DESC, id DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<Terms> findLatestEffectiveTerms(
            @Param("termsType") String termsType,
            @Param("now") LocalDateTime now
    );
}
