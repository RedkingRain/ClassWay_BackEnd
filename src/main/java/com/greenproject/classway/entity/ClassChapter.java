package com.greenproject.classway.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_chapters")
@IdClass(ClassChapterId.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClassChapter {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity clazz;

    @Id
    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
