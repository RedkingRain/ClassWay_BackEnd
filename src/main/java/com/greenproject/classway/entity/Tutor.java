package com.greenproject.classway.entity;

import com.greenproject.classway.global.TutorApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tutors")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Tutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tutor_id")
    private Integer tutorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(columnDefinition = "TEXT")
    private String career;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "document_url", length = 255)
    private String documentUrl;

    @Column(name = "approval_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TutorApprovalStatus approvalStatus = TutorApprovalStatus.PENDING;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", nullable = false)
    private User approvedBy;

    @Column(name = "applied_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
