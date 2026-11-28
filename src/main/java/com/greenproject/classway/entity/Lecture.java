package com.greenproject.classway.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lectures")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecture_id")
    private Integer lectureId;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(name = "chapter_id", nullable = false)
    private Integer chapterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "class_id", referencedColumnName = "class_id", insertable = false, updatable = false),
            @JoinColumn(name = "chapter_id", referencedColumnName = "chapter_id", insertable = false, updatable = false)
    })
    private ClassChapter chapter;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "video_url", length = 255)
    private String videoUrl;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "is_preview", nullable = false)
    private Boolean isPreview = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
