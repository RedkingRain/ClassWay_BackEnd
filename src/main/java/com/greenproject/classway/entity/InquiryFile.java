package com.greenproject.classway.entity;

import com.greenproject.classway.global.FileStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry_files")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InquiryFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, unique = true, length = 255)
    private String fileUrl;

    @Column(nullable = false, length = 6)
    @Enumerated(EnumType.STRING)
    private FileStatus status = FileStatus.TEMP;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
