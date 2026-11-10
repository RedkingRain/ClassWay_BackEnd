package com.greenproject.classway.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
