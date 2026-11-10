package com.greenproject.classway.entity;

import com.greenproject.classway.global.ClassStatus;
import com.greenproject.classway.global.ClassType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "classes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Integer classId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String summary;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(name = "class_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ClassType classType;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String region;

    @Column(name = "access_days")
    private Integer accessDays;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ClassStatus status = ClassStatus.DRAFT;

    @Column(name = "wish_count", nullable = false)
    private Integer wishCount = 0;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "avg_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
