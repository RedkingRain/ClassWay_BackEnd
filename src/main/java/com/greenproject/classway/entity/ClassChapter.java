package com.greenproject.classway.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_chapters")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClassChapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chapter_id")
    private Integer chapterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity clazz;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
