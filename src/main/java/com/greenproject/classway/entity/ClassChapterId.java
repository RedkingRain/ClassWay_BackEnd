package com.greenproject.classway.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClassChapterId implements Serializable {
    private Integer clazz;      // ClassChapter.clazz (class_id)
    private Integer chapterId;  // ClassChapter.chapterId (chapter_id)
}
